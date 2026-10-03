package com.mamang.datameter.domain.model

data class AppUsage(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L,
    val isSystemApp: Boolean = false
) {
    val mobileTotalBytes: Long
        get() = mobileRxBytes + mobileTxBytes

    val wifiTotalBytes: Long
        get() = wifiRxBytes + wifiTxBytes

    val rxBytes: Long
        get() = mobileRxBytes + wifiRxBytes

    val txBytes: Long
        get() = mobileTxBytes + wifiTxBytes

    val totalBytes: Long
        get() = rxBytes + txBytes
}

enum class AppUsageFilter {
    ALL,
    MOBILE,
    WIFI
}
