package com.mamang.datameter.domain.usecase

import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.first

class CheckQuotaAlertsUseCase(
    private val getQuotaStatusUseCase: GetQuotaStatusUseCase,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(
        onTriggerAlert: suspend (threshold: Int, usedBytes: Long, limitBytes: Long) -> Unit
    ) {
        val userSettings = preferencesRepository.userSettingsFlow.first()
        if (!userSettings.notificationsEnabled) return

        val quotaSettings = preferencesRepository.quotaSettingsFlow.first()
        if (!quotaSettings.isEnabled || quotaSettings.limitBytes <= 0L) return

        val quotaStatusResult = getQuotaStatusUseCase(quotaSettings)
        val status = quotaStatusResult.getOrNull() ?: return
        if (!status.isEnabled) return

        val cycleStart = status.billingCycleStart
        val currentPercent = status.percentage

        val thresholds = listOf(
            Pair(50, quotaSettings.warnAt50),
            Pair(75, quotaSettings.warnAt75),
            Pair(90, quotaSettings.warnAt90),
            Pair(100, quotaSettings.warnAt100)
        )

        for ((threshold, isConfigured) in thresholds) {
            if (isConfigured && currentPercent >= threshold) {
                val alreadyTriggered = preferencesRepository.hasQuotaAlertBeenTriggered(cycleStart, threshold)
                if (!alreadyTriggered) {
                    onTriggerAlert(threshold, status.usedBytes, status.limitBytes)
                    preferencesRepository.recordQuotaAlert(
                        cycleStart = cycleStart,
                        threshold = threshold,
                        usedBytes = status.usedBytes,
                        limitBytes = status.limitBytes
                    )
                }
            }
        }
    }
}
