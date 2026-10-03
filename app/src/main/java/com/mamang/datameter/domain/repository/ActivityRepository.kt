package com.mamang.datameter.domain.repository

import com.mamang.datameter.domain.model.NetworkActivity
import com.mamang.datameter.domain.model.NetworkActivityType
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {
    fun getRecentActivities(limit: Int = 5): Flow<List<NetworkActivity>>

    fun getActivities(
        startTime: Long,
        endTime: Long,
        networkType: NetworkActivityType?,
        searchQuery: String,
        isAscending: Boolean
    ): Flow<List<NetworkActivity>>

    suspend fun recordSnapshot(): Int

    suspend fun runRetentionCleanup()

    suspend fun clearAllHistory()
}
