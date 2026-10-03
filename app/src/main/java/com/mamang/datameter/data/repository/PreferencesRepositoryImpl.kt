package com.mamang.datameter.data.repository

import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.data.local.datastore.PreferencesManager
import com.mamang.datameter.data.local.room.QuotaAlertDao
import com.mamang.datameter.data.local.room.QuotaAlertEntity
import com.mamang.datameter.domain.model.AppTheme
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.UserSettings
import com.mamang.datameter.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class PreferencesRepositoryImpl(
    private val preferencesManager: PreferencesManager,
    private val quotaAlertDao: QuotaAlertDao
) : PreferencesRepository {

    override val userSettingsFlow: Flow<UserSettings> = preferencesManager.userSettingsFlow

    override val quotaSettingsFlow: Flow<QuotaSettings> = preferencesManager.quotaSettingsFlow

    override suspend fun updateTheme(theme: AppTheme) {
        preferencesManager.updateTheme(theme)
    }

    override suspend fun updateDataUnitFormat(format: DataUnitFormat) {
        preferencesManager.updateDataUnitFormat(format)
    }

    override suspend fun updateDefaultPeriod(period: PeriodType) {
        preferencesManager.updateDefaultPeriod(period)
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        preferencesManager.updateNotificationsEnabled(enabled)
    }

    override suspend fun updateQuotaSettings(settings: QuotaSettings) {
        preferencesManager.updateQuotaSettings(settings)
    }

    override suspend fun resetAllSettings() {
        preferencesManager.resetAllSettings()
        quotaAlertDao.clearAll()
    }

    override suspend fun recordQuotaAlert(
        cycleStart: Long,
        threshold: Int,
        usedBytes: Long,
        limitBytes: Long
    ) {
        quotaAlertDao.insertAlert(
            QuotaAlertEntity(
                cycleStart = cycleStart,
                thresholdPercent = threshold,
                triggeredAt = System.currentTimeMillis(),
                usedBytes = usedBytes,
                limitBytes = limitBytes
            )
        )
    }

    override suspend fun hasQuotaAlertBeenTriggered(cycleStart: Long, threshold: Int): Boolean {
        return quotaAlertDao.getAlert(cycleStart, threshold) != null
    }
}
