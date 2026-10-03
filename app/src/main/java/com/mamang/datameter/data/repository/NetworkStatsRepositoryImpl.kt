package com.mamang.datameter.data.repository

import com.mamang.datameter.data.networkstats.NetworkStatsDataSource
import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.model.UsageSummary
import com.mamang.datameter.domain.repository.NetworkStatsRepository

class NetworkStatsRepositoryImpl(
    private val dataSource: NetworkStatsDataSource
) : NetworkStatsRepository {

    override suspend fun getUsageSummary(startTime: Long, endTime: Long): Result<UsageSummary> {
        return try {
            val summary = dataSource.getDeviceUsageSummary(startTime, endTime)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDailyUsagePoints(startTime: Long, endTime: Long): Result<List<DailyUsagePoint>> {
        return try {
            val points = dataSource.getDailyUsagePoints(startTime, endTime)
            Result.success(points)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAppUsageList(startTime: Long, endTime: Long): Result<List<AppUsage>> {
        return try {
            val apps = dataSource.getAppUsageList(startTime, endTime)
            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
