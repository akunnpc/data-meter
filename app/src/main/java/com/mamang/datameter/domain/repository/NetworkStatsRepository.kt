package com.mamang.datameter.domain.repository

import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.model.UsageSummary

interface NetworkStatsRepository {
    suspend fun getUsageSummary(startTime: Long, endTime: Long): Result<UsageSummary>
    suspend fun getDailyUsagePoints(startTime: Long, endTime: Long): Result<List<DailyUsagePoint>>
    suspend fun getAppUsageList(startTime: Long, endTime: Long): Result<List<AppUsage>>
}
