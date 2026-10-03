package com.mamang.datameter.data.repository

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import com.mamang.datameter.core.utils.AppDetailsResolver
import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.core.utils.PermissionHelper
import com.mamang.datameter.data.local.room.AppSnapshotEntity
import com.mamang.datameter.data.local.room.NetworkActivityDao
import com.mamang.datameter.data.local.room.NetworkActivityEntity
import com.mamang.datameter.domain.model.NetworkActivity
import com.mamang.datameter.domain.model.NetworkActivityType
import com.mamang.datameter.domain.repository.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActivityRepositoryImpl(
    private val context: Context,
    private val activityDao: NetworkActivityDao
) : ActivityRepository {

    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    override fun getRecentActivities(limit: Int): Flow<List<NetworkActivity>> {
        return activityDao.getRecentActivities(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActivities(
        startTime: Long,
        endTime: Long,
        networkType: NetworkActivityType?,
        searchQuery: String,
        isAscending: Boolean
    ): Flow<List<NetworkActivity>> {
        val networkTypeStr = networkType?.name
        return activityDao.getActivitiesFiltered(
            startTime = startTime,
            endTime = endTime,
            networkType = networkTypeStr,
            searchQuery = searchQuery.trim(),
            isAsc = isAscending
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun recordSnapshot(): Int = withContext(Dispatchers.IO) {
        val nsm = networkStatsManager ?: return@withContext 0
        if (!PermissionHelper.hasUsageStatsPermission(context)) return@withContext 0

        val now = System.currentTimeMillis()
        // Query cumulative usage for the current 24-hour cycle
        val windowStart = DateUtils.getTimeRange(PeriodType.TODAY).startTimeMillis
        if (windowStart >= now) return@withContext 0

        // Fetch existing snapshots
        val previousSnapshots = activityDao.getAllSnapshots().associateBy { "${it.uid}-${it.networkType}" }

        data class UidStat(val rx: Long, val tx: Long)
        val mobileCurrent = mutableMapOf<Int, UidStat>()
        val wifiCurrent = mutableMapOf<Int, UidStat>()

        // 1. Mobile query
        try {
            @Suppress("DEPRECATION")
            val stats = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, windowStart, now)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val prev = mobileCurrent[bucket.uid]
                val rx = (prev?.rx ?: 0L) + bucket.rxBytes.coerceAtLeast(0L)
                val tx = (prev?.tx ?: 0L) + bucket.txBytes.coerceAtLeast(0L)
                mobileCurrent[bucket.uid] = UidStat(rx, tx)
            }
            stats.close()
        } catch (_: Exception) {
        }

        // 2. Wi-Fi query
        try {
            @Suppress("DEPRECATION")
            val stats = nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, windowStart, now)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val prev = wifiCurrent[bucket.uid]
                val rx = (prev?.rx ?: 0L) + bucket.rxBytes.coerceAtLeast(0L)
                val tx = (prev?.tx ?: 0L) + bucket.txBytes.coerceAtLeast(0L)
                wifiCurrent[bucket.uid] = UidStat(rx, tx)
            }
            stats.close()
        } catch (_: Exception) {
        }

        val newActivities = mutableListOf<NetworkActivityEntity>()
        val updatedSnapshots = mutableListOf<AppSnapshotEntity>()
        val installedAppsByUid = AppDetailsResolver.getInstalledAppsMap(context)

        fun processType(networkType: String, currentMap: Map<Int, UidStat>) {
            for ((uid, stat) in currentMap) {
                if (stat.rx <= 0L && stat.tx <= 0L) continue

                val key = "$uid-$networkType"
                val previous = previousSnapshots[key]

                if (previous != null) {
                    val deltaRx = stat.rx - previous.lastRxBytes
                    val deltaTx = stat.tx - previous.lastTxBytes

                    // If delta is positive and usage occurred
                    if ((deltaRx > 0L || deltaTx > 0L) && deltaRx >= 0L && deltaTx >= 0L) {
                        val resolved = AppDetailsResolver.resolve(context, uid, installedAppsByUid)
                        newActivities.add(
                            NetworkActivityEntity(
                                timestamp = now,
                                uid = uid,
                                packageName = resolved.packageName,
                                appName = resolved.appName,
                                networkType = networkType,
                                rxBytes = deltaRx,
                                txBytes = deltaTx,
                                totalBytes = deltaRx + deltaTx,
                                aggregationLevel = 0
                            )
                        )
                    }

                    // Update snapshot with latest counter
                    updatedSnapshots.add(
                        AppSnapshotEntity(
                            uid = uid,
                            networkType = networkType,
                            lastRxBytes = stat.rx,
                            lastTxBytes = stat.tx,
                            lastSnapshotTime = now
                        )
                    )
                } else {
                    // First observation for this UID: establish baseline snapshot without logging past bulk
                    updatedSnapshots.add(
                        AppSnapshotEntity(
                            uid = uid,
                            networkType = networkType,
                            lastRxBytes = stat.rx,
                            lastTxBytes = stat.tx,
                            lastSnapshotTime = now
                        )
                    )
                }
            }
        }

        processType("CELLULAR", mobileCurrent)
        processType("WIFI", wifiCurrent)

        if (newActivities.isNotEmpty()) {
            activityDao.insertActivities(newActivities)
        }
        if (updatedSnapshots.isNotEmpty()) {
            activityDao.upsertSnapshots(updatedSnapshots)
        }

        newActivities.size
    }

    override suspend fun runRetentionCleanup(): Unit = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val oneHourMillis = 3600000L
        val oneDayMillis = 86400000L

        // 1. Roll up raw minute records older than 7 days into hourly records
        val cutoff7Days = now - (7 * oneDayMillis)
        val rawOlder = activityDao.getRawActivitiesOlderThan(cutoff7Days)
        if (rawOlder.isNotEmpty()) {
            data class GroupKey(
                val hour: Long,
                val uid: Int,
                val packageName: String,
                val appName: String,
                val networkType: String
            )

            val grouped = rawOlder.groupBy {
                GroupKey(
                    hour = (it.timestamp / oneHourMillis) * oneHourMillis,
                    uid = it.uid,
                    packageName = it.packageName,
                    appName = it.appName,
                    networkType = it.networkType
                )
            }

            val hourlyEntities = grouped.map { (key, list) ->
                val sumRx = list.sumOf { it.rxBytes }
                val sumTx = list.sumOf { it.txBytes }
                NetworkActivityEntity(
                    timestamp = key.hour,
                    uid = key.uid,
                    packageName = key.packageName,
                    appName = key.appName,
                    networkType = key.networkType,
                    rxBytes = sumRx,
                    txBytes = sumTx,
                    totalBytes = sumRx + sumTx,
                    aggregationLevel = 1
                )
            }

            activityDao.insertActivities(hourlyEntities)
            activityDao.deleteActivitiesByIds(rawOlder.map { it.id })
        }

        // 2. Roll up hourly records older than 30 days into daily records
        val cutoff30Days = now - (30 * oneDayMillis)
        val hourlyOlder = activityDao.getHourlyActivitiesOlderThan(cutoff30Days)
        if (hourlyOlder.isNotEmpty()) {
            data class DayGroupKey(
                val day: Long,
                val uid: Int,
                val packageName: String,
                val appName: String,
                val networkType: String
            )

            val groupedDaily = hourlyOlder.groupBy {
                DayGroupKey(
                    day = (it.timestamp / oneDayMillis) * oneDayMillis,
                    uid = it.uid,
                    packageName = it.packageName,
                    appName = it.appName,
                    networkType = it.networkType
                )
            }

            val dailyEntities = groupedDaily.map { (key, list) ->
                val sumRx = list.sumOf { it.rxBytes }
                val sumTx = list.sumOf { it.txBytes }
                NetworkActivityEntity(
                    timestamp = key.day,
                    uid = key.uid,
                    packageName = key.packageName,
                    appName = key.appName,
                    networkType = key.networkType,
                    rxBytes = sumRx,
                    txBytes = sumTx,
                    totalBytes = sumRx + sumTx,
                    aggregationLevel = 2
                )
            }

            activityDao.insertActivities(dailyEntities)
            activityDao.deleteActivitiesByIds(hourlyOlder.map { it.id })
        }

        // 3. Purge records older than 90 days
        val cutoff90Days = now - (90 * oneDayMillis)
        activityDao.deleteActivitiesOlderThan(cutoff90Days)
    }

    override suspend fun clearAllHistory(): Unit = withContext(Dispatchers.IO) {
        activityDao.clearAllActivities()
        activityDao.clearAllSnapshots()
    }

    private fun NetworkActivityEntity.toDomain(): NetworkActivity {
        return NetworkActivity(
            id = id,
            timestamp = timestamp,
            uid = uid,
            packageName = packageName,
            appName = appName,
            networkType = NetworkActivityType.fromString(networkType),
            rxBytes = rxBytes,
            txBytes = txBytes,
            totalBytes = totalBytes,
            formattedTime = DateUtils.formatDate(timestamp, "HH:mm:ss"),
            formattedDate = DateUtils.formatDate(timestamp, "dd MMM yyyy")
        )
    }
}
