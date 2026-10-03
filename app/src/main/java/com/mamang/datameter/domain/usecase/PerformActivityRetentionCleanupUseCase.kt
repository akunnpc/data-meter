package com.mamang.datameter.domain.usecase

import com.mamang.datameter.domain.repository.ActivityRepository

class PerformActivityRetentionCleanupUseCase(
    private val repository: ActivityRepository
) {
    suspend operator fun invoke() {
        repository.runRetentionCleanup()
    }
}
