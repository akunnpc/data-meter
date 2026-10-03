package com.mamang.datameter.domain.model

data class UsageSummary(
    val mobileUsage: NetworkUsage = NetworkUsage.ZERO,
    val wifiUsage: NetworkUsage = NetworkUsage.ZERO,
    val periodLabel: String = ""
) {
    val totalUsage: NetworkUsage
        get() = mobileUsage + wifiUsage

    val hasAnyData: Boolean
        get() = totalUsage.totalBytes > 0L

    companion object {
        val EMPTY = UsageSummary()
    }
}
