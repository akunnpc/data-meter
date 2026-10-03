package com.mamang.datameter.domain.usecase

import com.mamang.datameter.domain.model.NetworkActivity
import com.mamang.datameter.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

class GetRecentActivitiesUseCase(
    private val repository: ActivityRepository
) {
    operator fun invoke(limit: Int = 5): Flow<List<NetworkActivity>> {
        return repository.getRecentActivities(limit)
    }
}
