package com.mamang.datameter.domain.usecase

import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.UsageSummary
import com.mamang.datameter.domain.repository.NetworkStatsRepository

class GetNetworkUsageUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(
        periodType: PeriodType,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        resetDay: Int = 1
    ): Result<Pair<UsageSummary, String>> {
        val range = DateUtils.getTimeRange(
            periodType = periodType,
            customStartMillis = customStartMillis,
            customEndMillis = customEndMillis,
            billingCycleResetDay = resetDay
        )

        return repository.getUsageSummary(range.startTimeMillis, range.endTimeMillis)
            .map { summary ->
                Pair(summary.copy(periodLabel = range.label), range.label)
            }
    }
}
