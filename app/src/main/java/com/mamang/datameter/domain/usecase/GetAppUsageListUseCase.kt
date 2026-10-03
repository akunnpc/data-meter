package com.mamang.datameter.domain.usecase

import com.mamang.datameter.core.utils.DateUtils
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.AppUsageFilter
import com.mamang.datameter.domain.repository.NetworkStatsRepository

class GetAppUsageListUseCase(
    private val repository: NetworkStatsRepository
) {
    suspend operator fun invoke(
        periodType: PeriodType,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        resetDay: Int = 1,
        filter: AppUsageFilter = AppUsageFilter.ALL,
        searchQuery: String = ""
    ): Result<List<AppUsage>> {
        val range = DateUtils.getTimeRange(
            periodType = periodType,
            customStartMillis = customStartMillis,
            customEndMillis = customEndMillis,
            billingCycleResetDay = resetDay
        )

        return repository.getAppUsageList(range.startTimeMillis, range.endTimeMillis)
            .map { list ->
                list.filter { item ->
                    // Apply Search Filter
                    val matchesSearch = if (searchQuery.isBlank()) {
                        true
                    } else {
                        item.appName.contains(searchQuery, ignoreCase = true) ||
                                item.packageName.contains(searchQuery, ignoreCase = true)
                    }

                    // Apply Network Filter
                    val hasUsageInFilter = when (filter) {
                        AppUsageFilter.ALL -> item.totalBytes > 0L
                        AppUsageFilter.MOBILE -> item.mobileTotalBytes > 0L
                        AppUsageFilter.WIFI -> item.wifiTotalBytes > 0L
                    }

                    matchesSearch && hasUsageInFilter
                }.sortedWith { a, b ->
                    val usageA = when (filter) {
                        AppUsageFilter.ALL -> a.totalBytes
                        AppUsageFilter.MOBILE -> a.mobileTotalBytes
                        AppUsageFilter.WIFI -> a.wifiTotalBytes
                    }
                    val usageB = when (filter) {
                        AppUsageFilter.ALL -> b.totalBytes
                        AppUsageFilter.MOBILE -> b.mobileTotalBytes
                        AppUsageFilter.WIFI -> b.wifiTotalBytes
                    }
                    usageB.compareTo(usageA)
                }
            }
    }
}
