package com.mamang.datameter.core.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class ResolvedAppInfo(
    val appName: String,
    val packageName: String,
    val isSystemApp: Boolean
)

object AppDetailsResolver {

    fun getInstalledAppsMap(context: Context): Map<Int, ApplicationInfo> {
        return try {
            val pm = context.packageManager
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }
            apps.associateBy { it.uid }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun resolve(
        context: Context,
        uid: Int,
        installedAppsByUid: Map<Int, ApplicationInfo>
    ): ResolvedAppInfo {
        val pm = context.packageManager

        // 1. Check known system-level UIDs
        when (uid) {
            0 -> return ResolvedAppInfo("Sistem Android (Kernel)", "android.kernel", true)
            1000 -> return ResolvedAppInfo("Sistem Android (OS)", "android.os", true)
            1001 -> return ResolvedAppInfo("Telefoni & Radio", "android.phone", true)
            1013 -> return ResolvedAppInfo("Media Server", "android.media", true)
            1021 -> return ResolvedAppInfo("GPS & Layanan Lokasi", "android.location", true)
            1073 -> return ResolvedAppInfo("Pengelola Unduhan", "com.android.providers.downloads", true)
            -4 -> return ResolvedAppInfo("Aplikasi yang Dihapus", "android.removed", false)
            -5 -> return ResolvedAppInfo("Tethering & Hotspot", "android.tethering", false)
        }

        // 2. Direct lookup from installed apps cache
        installedAppsByUid[uid]?.let { appInfo ->
            val label = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (_: Exception) {
                ""
            }
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            return ResolvedAppInfo(
                appName = if (label.isNotBlank()) label else appInfo.packageName,
                packageName = appInfo.packageName,
                isSystemApp = isSystem
            )
        }

        // 3. Query packages for UID
        val packages = try {
            pm.getPackagesForUid(uid)
        } catch (_: Exception) {
            null
        }

        if (!packages.isNullOrEmpty()) {
            for (pkg in packages) {
                try {
                    val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getApplicationInfo(pkg, 0)
                    }
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    if (label.isNotBlank()) {
                        return ResolvedAppInfo(label, pkg, isSystem)
                    }
                } catch (_: Exception) {
                }
            }
            val primaryPkg = packages[0]
            return ResolvedAppInfo(primaryPkg, primaryPkg, false)
        }

        // 4. Fallback to getNameForUid
        val nameForUid = try {
            pm.getNameForUid(uid)
        } catch (_: Exception) {
            null
        }

        if (!nameForUid.isNullOrBlank()) {
            try {
                val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(nameForUid, PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(nameForUid, 0)
                }
                val label = pm.getApplicationLabel(appInfo).toString()
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                return ResolvedAppInfo(
                    appName = if (label.isNotBlank()) label else nameForUid,
                    packageName = nameForUid,
                    isSystemApp = isSystem
                )
            } catch (_: Exception) {
                if (!nameForUid.startsWith("uid:")) {
                    return ResolvedAppInfo(nameForUid, nameForUid, false)
                }
            }
        }

        // 5. Final fallback
        return if (uid >= 10000) {
            ResolvedAppInfo("Aplikasi Dihapus (UID $uid)", "android.uid.$uid", false)
        } else {
            ResolvedAppInfo("Layanan Sistem (UID $uid)", "android.uid.$uid", true)
        }
    }
}
