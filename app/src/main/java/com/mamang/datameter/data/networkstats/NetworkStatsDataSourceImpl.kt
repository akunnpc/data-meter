package com.mamang.datameter.data.networkstats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
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
            val installedAppsByUid: Map<Int, ApplicationInfo> = try {
                val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getInstalledApplications(0)
                }
                apps.associateBy { it.uid }
            } catch (_: Exception) {
                emptyMap()
            }

            for ((uid, acc) in uidUsageMap) {
                val totalBytes = acc.mobileRx + acc.mobileTx + acc.wifiRx + acc.wifiTx
                if (totalBytes <= 0L) continue

                val (appName, packageName, isSystem) = resolveAppDetails(uid, installedAppsByUid)
                result.add(
                    AppUsage(
                        uid = uid,
                        packageName = packageName,
                        appName = appName,
                        mobileRxBytes = acc.mobileRx,
                        mobileTxBytes = acc.mobileTx,
                        wifiRxBytes = acc.wifiRx,
                        wifiTxBytes = acc.wifiTx,
                        isSystemApp = isSystem
                    )
                )
            }

            // Sort by total bytes descending
            result.sortByDescending { it.totalBytes }
            result
        }

    private fun resolveAppDetails(
        uid: Int,
        installedAppsByUid: Map<Int, ApplicationInfo>
    ): Triple<String, String, Boolean> {
        // 1. Check known system-level UIDs
        when (uid) {
            0 -> return Triple("Sistem Android (Kernel)", "android.kernel", true)
            1000 -> return Triple("Sistem Android (OS)", "android.os", true)
            1001 -> return Triple("Telefoni & Radio", "android.phone", true)
            1013 -> return Triple("Media Server (Audio/Video)", "android.media", true)
            1021 -> return Triple("GPS & Layanan Lokasi", "android.location", true)
            1073 -> return Triple("Pengelola Unduhan", "com.android.providers.downloads", true)
            -4 -> return Triple("Aplikasi yang Dihapus", "android.removed", false)
            -5 -> return Triple("Tethering & Hotspot", "android.tethering", false)
        }

        // 2. Direct lookup from installed apps cache by UID
        installedAppsByUid[uid]?.let { appInfo ->
            val label = try {
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                ""
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            return Triple(
                if (label.isNotBlank()) label else appInfo.packageName,
                appInfo.packageName,
                isSystem
            )
        }

        // 3. Query packages assigned to this UID
        val packages = try {
            packageManager.getPackagesForUid(uid)
        } catch (_: Exception) {
            null
        }

        if (!packages.isNullOrEmpty()) {
            for (pkg in packages) {
                try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getApplicationInfo(pkg, 0)
                    }
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (label.isNotBlank()) {
                        return Triple(label, pkg, isSystem)
                    }
                } catch (_: Exception) {
                    // Try next package in shared UID
                }
            }
            val primaryPkg = packages[0]
            return Triple(primaryPkg, primaryPkg, false)
        }

        // 4. Fallback to getNameForUid
        val nameForUid = try {
            packageManager.getNameForUid(uid)
        } catch (_: Exception) {
            null
        }

        if (!nameForUid.isNullOrBlank()) {
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(nameForUid, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(nameForUid, 0)
                }
                val label = packageManager.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                return Triple(if (label.isNotBlank()) label else nameForUid, nameForUid, isSystem)
            } catch (_: Exception) {
                // Return clean name
                if (!nameForUid.startsWith("uid:")) {
                    return Triple(nameForUid, nameForUid, false)
                }
            }
        }

        // 5. Final fallback: distinguish uninstalled user app from system daemon
        return if (uid >= 10000) {
            Triple("Aplikasi Dihapus (UID $uid)", "android.uid.$uid", false)
        } else {
            Triple("Layanan Sistem (UID $uid)", "android.uid.$uid", true)
        }
    }
}
