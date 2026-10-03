package com.mamang.datameter.data.networkstats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import com.mamang.datameter.core.utils.AppDetailsResolver
import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.model.NetworkUsage
import com.mamang.datameter.domain.model.UsageSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone

class NetworkStatsDataSourceImpl(
    private val context: Context
) : NetworkStatsDataSource {

    private val networkStatsManager: NetworkStatsManager? =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    private val packageManager: PackageManager = context.packageManager

    override suspend fun getDeviceUsageSummary(startTime: Long, endTime: Long): UsageSummary =
        withContext(Dispatchers.IO) {
            val nsm = networkStatsManager ?: return@withContext UsageSummary.EMPTY
            if (startTime >= endTime) return@withContext UsageSummary.EMPTY

            val mobileUsage = queryDeviceUsage(nsm, ConnectivityManager.TYPE_MOBILE, startTime, endTime)
            val wifiUsage = queryDeviceUsage(nsm, ConnectivityManager.TYPE_WIFI, startTime, endTime)

            UsageSummary(
                mobileUsage = mobileUsage,
                wifiUsage = wifiUsage
            )
        }

    private fun queryDeviceUsage(
        nsm: NetworkStatsManager,
        networkType: Int,
        startTime: Long,
        endTime: Long
    ): NetworkUsage {
        return try {
            val bucket = nsm.querySummaryForDevice(networkType, null, startTime, endTime)
            NetworkUsage(
                rxBytes = bucket.rxBytes.coerceAtLeast(0L),
                txBytes = bucket.txBytes.coerceAtLeast(0L)
            )
        } catch (_: SecurityException) {
            NetworkUsage.ZERO
        } catch (_: IllegalArgumentException) {
            NetworkUsage.ZERO
        } catch (_: Exception) {
            NetworkUsage.ZERO
        }
    }

    override suspend fun getDailyUsagePoints(startTime: Long, endTime: Long): List<DailyUsagePoint> =
        withContext(Dispatchers.IO) {
            val nsm = networkStatsManager ?: return@withContext emptyList()
            if (startTime >= endTime) return@withContext emptyList()

            val durationMillis = endTime - startTime
            val oneDayMillis = 24 * 60 * 60 * 1000L

            val points = mutableListOf<DailyUsagePoint>()

            if (durationMillis <= oneDayMillis + 1000L) {
                // Today or <= 24 hours: Break down by 3-hour or 4-hour intervals
                val stepMillis = 4 * 60 * 60 * 1000L
                var stepStart = startTime
                while (stepStart < endTime) {
                    val stepEnd = (stepStart + stepMillis).coerceAtMost(endTime)
                    val mobile = queryDeviceUsage(nsm, ConnectivityManager.TYPE_MOBILE, stepStart, stepEnd)
                    val wifi = queryDeviceUsage(nsm, ConnectivityManager.TYPE_WIFI, stepStart, stepEnd)
                    val label = DateUtils.formatHour(stepStart)

                    points.add(
                        DailyUsagePoint(
                            timestamp = stepStart,
                            dateLabel = label,
                            mobileRxBytes = mobile.rxBytes,
                            mobileTxBytes = mobile.txBytes,
                            wifiRxBytes = wifi.rxBytes,
                            wifiTxBytes = wifi.txBytes
                        )
                    )
                    stepStart = stepEnd
                }
            } else {
                // Multi-day: Break down by calendar days
                val calendar = Calendar.getInstance(TimeZone.getDefault())
                calendar.timeInMillis = startTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                while (calendar.timeInMillis < endTime) {
                    val dayStart = calendar.timeInMillis.coerceAtLeast(startTime)
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    val dayEnd = calendar.timeInMillis.coerceAtMost(endTime)

                    val mobile = queryDeviceUsage(nsm, ConnectivityManager.TYPE_MOBILE, dayStart, dayEnd)
                    val wifi = queryDeviceUsage(nsm, ConnectivityManager.TYPE_WIFI, dayStart, dayEnd)
                    val label = DateUtils.formatDay(dayStart)

                    points.add(
                        DailyUsagePoint(
                            timestamp = dayStart,
                            dateLabel = label,
                            mobileRxBytes = mobile.rxBytes,
                            mobileTxBytes = mobile.txBytes,
                            wifiRxBytes = wifi.rxBytes,
                            wifiTxBytes = wifi.txBytes
                        )
                    )
                }
            }

            points
        }

    override suspend fun getAppUsageList(startTime: Long, endTime: Long): List<AppUsage> =
        withContext(Dispatchers.IO) {
            val nsm = networkStatsManager ?: return@withContext emptyList()
            if (startTime >= endTime) return@withContext emptyList()

            data class UidAccumulator(
                var mobileRx: Long = 0L,
                var mobileTx: Long = 0L,
                var wifiRx: Long = 0L,
                var wifiTx: Long = 0L
            )

            val uidUsageMap = mutableMapOf<Int, UidAccumulator>()

            // Query Mobile
            try {
                val mobileStats = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
                val bucket = NetworkStats.Bucket()
                while (mobileStats.hasNextBucket()) {
                    mobileStats.getNextBucket(bucket)
                    val acc = uidUsageMap.getOrPut(bucket.uid) { UidAccumulator() }
                    acc.mobileRx += bucket.rxBytes.coerceAtLeast(0L)
                    acc.mobileTx += bucket.txBytes.coerceAtLeast(0L)
                }
                mobileStats.close()
            } catch (_: Exception) {
                // If mobile query summary is not supported or permission denied
            }

            // Query Wi-Fi
            try {
                val wifiStats = nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
                val bucket = NetworkStats.Bucket()
                while (wifiStats.hasNextBucket()) {
                    wifiStats.getNextBucket(bucket)
                    val acc = uidUsageMap.getOrPut(bucket.uid) { UidAccumulator() }
                    acc.wifiRx += bucket.rxBytes.coerceAtLeast(0L)
                    acc.wifiTx += bucket.txBytes.coerceAtLeast(0L)
                }
                wifiStats.close()
            } catch (_: Exception) {
                // If wifi query summary is not supported or permission denied
            }

            val result = mutableListOf<AppUsage>()

            // Pre-cache all installed applications by UID to quickly resolve names and icons
            val installedAppsByUid = AppDetailsResolver.getInstalledAppsMap(context)

            for ((uid, acc) in uidUsageMap) {
                val totalBytes = acc.mobileRx + acc.mobileTx + acc.wifiRx + acc.wifiTx
                if (totalBytes <= 0L) continue

                val resolved = AppDetailsResolver.resolve(context, uid, installedAppsByUid)
                result.add(
                    AppUsage(
                        uid = uid,
                        packageName = resolved.packageName,
                        appName = resolved.appName,
                        mobileRxBytes = acc.mobileRx,
                        mobileTxBytes = acc.mobileTx,
                        wifiRxBytes = acc.wifiRx,
                        wifiTxBytes = acc.wifiTx,
                        isSystemApp = resolved.isSystemApp
                    )
                )
            }

            // Sort by total bytes descending
            result.sortByDescending { it.totalBytes }
            result
        }
}
