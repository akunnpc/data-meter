package com.mamang.datameter.domain.model

data class DailyUsagePoint(
    val timestamp: Long,
    val dateLabel: String,
    val mobileRxBytes: Long = 0L,
    val mobileTxBytes: Long = 0L,
    val wifiRxBytes: Long = 0L,
    val wifiTxBytes: Long = 0L
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

    fun getBytesForFilter(filter: ChartDataFilter): Long {
        return when (filter) {
            ChartDataFilter.ALL -> totalBytes
            ChartDataFilter.MOBILE -> mobileTotalBytes
            ChartDataFilter.WIFI -> wifiTotalBytes
        }
    }
}

enum class ChartDataFilter {
    ALL,
    MOBILE,
    WIFI
}

enum class ChartMetricType {
    TOTAL,
    DOWNLOAD,
    UPLOAD
}
