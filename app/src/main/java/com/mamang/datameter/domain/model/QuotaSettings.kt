package com.mamang.datameter.domain.model

enum class QuotaResetPeriod {
    MONTHLY,
    WEEKLY,
    DAILY
}

data class QuotaSettings(
    val isEnabled: Boolean = false,
    val limitBytes: Long = 10L * 1024L * 1024L * 1024L, // Default 10 GB
    val resetPeriod: QuotaResetPeriod = QuotaResetPeriod.MONTHLY,
    val resetDay: Int = 1, // 1st of month
    val warnAt50: Boolean = true,
    val warnAt75: Boolean = true,
    val warnAt90: Boolean = true,
    val warnAt100: Boolean = true
)

data class QuotaStatus(
    val isEnabled: Boolean = false,
    val limitBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val remainingBytes: Long = 0L,
    val percentage: Float = 0f,
    val isExceeded: Boolean = false,
    val billingCycleStart: Long = 0L,
    val billingCycleEnd: Long = 0L,
    val billingCycleLabel: String = ""
)
