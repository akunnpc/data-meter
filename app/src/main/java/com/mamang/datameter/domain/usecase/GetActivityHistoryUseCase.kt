package com.mamang.datameter.domain.usecase

import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.NetworkActivity
import com.mamang.datameter.domain.model.NetworkActivityType
import com.mamang.datameter.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

class GetActivityHistoryUseCase(
    private val repository: ActivityRepository
) {
    operator fun invoke(
        periodType: PeriodType,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        networkType: NetworkActivityType? = null,
        searchQuery: String = "",
        isAscending: Boolean = false
    ): Flow<List<NetworkActivity>> {
        val range = DateUtils.getTimeRange(
            periodType = periodType,
            customStartMillis = customStartMillis,
            customEndMillis = customEndMillis
        )

        return repository.getActivities(
            startTime = range.startTimeMillis,
            endTime = range.endTimeMillis,
            networkType = networkType,
            searchQuery = searchQuery,
            isAscending = isAscending
        )
    }
}
