package com.mamang.datameter.domain.repository

import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.AppTheme
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val userSettingsFlow: Flow<UserSettings>
    val quotaSettingsFlow: Flow<QuotaSettings>

    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateDataUnitFormat(format: DataUnitFormat)
    suspend fun updateDefaultPeriod(period: PeriodType)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateQuotaSettings(settings: QuotaSettings)
    suspend fun resetAllSettings()

    suspend fun recordQuotaAlert(cycleStart: Long, threshold: Int, usedBytes: Long, limitBytes: Long)
    suspend fun hasQuotaAlertBeenTriggered(cycleStart: Long, threshold: Int): Boolean
}
