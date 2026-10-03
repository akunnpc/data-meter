package com.mamang.datameter.domain.usecase

import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.domain.model.QuotaResetPeriod
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.QuotaStatus
import com.mamang.datameter.domain.repository.NetworkStatsRepository
import java.util.Calendar
import java.util.TimeZone

class GetQuotaStatusUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(quotaSettings: QuotaSettings): Result<QuotaStatus> {
        if (!quotaSettings.isEnabled || quotaSettings.limitBytes <= 0L) {
            return Result.success(
                QuotaStatus(
                    isEnabled = false,
                    limitBytes = quotaSettings.limitBytes
                )
            )
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getDefault())

        val (cycleStart, cycleEnd) = when (quotaSettings.resetPeriod) {
            QuotaResetPeriod.DAILY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            QuotaResetPeriod.WEEKLY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                Pair(start, calendar.timeInMillis)
            }
            QuotaResetPeriod.MONTHLY -> {
                DateUtils.getBillingCycleRange(now, quotaSettings.resetDay)
            }
        }

        return repository.getUsageSummary(cycleStart, now).map { summary ->
            val usedBytes = summary.mobileUsage.totalBytes
            val limitBytes = quotaSettings.limitBytes
            val remainingBytes = (limitBytes - usedBytes).coerceAtLeast(0L)
            val percentage = ((usedBytes.toDouble() / limitBytes.toDouble()) * 100.0)
                .toFloat()
                .coerceAtLeast(0f)
            val isExceeded = usedBytes >= limitBytes

            val label = "${DateUtils.formatDate(cycleStart, "dd MMM")} - ${DateUtils.formatDate(cycleEnd, "dd MMM")}"

            QuotaStatus(
                isEnabled = true,
                limitBytes = limitBytes,
                usedBytes = usedBytes,
                remainingBytes = remainingBytes,
                percentage = percentage,
                isExceeded = isExceeded,
                billingCycleStart = cycleStart,
                billingCycleEnd = cycleEnd,
                billingCycleLabel = label
            )
        }
    }
}
