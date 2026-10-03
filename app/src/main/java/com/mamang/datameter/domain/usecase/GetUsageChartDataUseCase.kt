package com.mamang.datameter.domain.usecase

import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.repository.NetworkStatsRepository

class GetUsageChartDataUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(
        periodType: PeriodType,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        resetDay: Int = 1
    ): Result<List<DailyUsagePoint>> {
        val range = DateUtils.getTimeRange(
            periodType = periodType,
            customStartMillis = customStartMillis,
            customEndMillis = customEndMillis,
            billingCycleResetDay = resetDay
        )

        return repository.getDailyUsagePoints(range.startTimeMillis, range.endTimeMillis)
    }
}
