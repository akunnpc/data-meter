package com.mamang.datameter.data.networkstats

import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.model.UsageSummary

interface NetworkStatsDataSource {
    suspend fun getDeviceUsageSummary(startTime: Long, endTime: Long): UsageSummary
    suspend fun getDailyUsagePoints(startTime: Long, endTime: Long): List<DailyUsagePoint>
    suspend fun getAppUsageList(startTime: Long, endTime: Long): List<AppUsage>
}
