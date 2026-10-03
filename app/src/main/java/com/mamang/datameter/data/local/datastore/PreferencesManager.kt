package com.mamang.datameter.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.AppTheme
import com.mamang.datameter.domain.model.QuotaResetPeriod
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "datameter_preferences")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val DATA_UNIT = stringPreferencesKey("data_unit")
        val DEFAULT_PERIOD = stringPreferencesKey("default_period")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")

        val QUOTA_ENABLED = booleanPreferencesKey("quota_enabled")
        val QUOTA_LIMIT_BYTES = longPreferencesKey("quota_limit_bytes")
        val QUOTA_RESET_PERIOD = stringPreferencesKey("quota_reset_period")
        val QUOTA_RESET_DAY = intPreferencesKey("quota_reset_day")
        val QUOTA_WARN_50 = booleanPreferencesKey("quota_warn_50")
        val QUOTA_WARN_75 = booleanPreferencesKey("quota_warn_75")
        val QUOTA_WARN_90 = booleanPreferencesKey("quota_warn_90")
        val QUOTA_WARN_100 = booleanPreferencesKey("quota_warn_100")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeStr = prefs[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name
            val theme = try {
                AppTheme.valueOf(themeStr)
            } catch (_: Exception) {
                AppTheme.SYSTEM
            }

            val unitStr = prefs[PreferencesKeys.DATA_UNIT] ?: DataUnitFormat.BINARY.name
            val unit = try {
                DataUnitFormat.valueOf(unitStr)
            } catch (_: Exception) {
                DataUnitFormat.BINARY
            }

            val periodStr = prefs[PreferencesKeys.DEFAULT_PERIOD] ?: PeriodType.TODAY.name
            val period = try {
                PeriodType.valueOf(periodStr)
            } catch (_: Exception) {
                PeriodType.TODAY
            }

            val notifEnabled = prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true

            UserSettings(
                theme = theme,
                dataUnitFormat = unit,
                defaultPeriod = period,
                notificationsEnabled = notifEnabled
            )
        }

    val quotaSettingsFlow: Flow<QuotaSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val isEnabled = prefs[PreferencesKeys.QUOTA_ENABLED] ?: false
            val limit = prefs[PreferencesKeys.QUOTA_LIMIT_BYTES] ?: (10L * 1024L * 1024L * 1024L)
            val resetPeriodStr = prefs[PreferencesKeys.QUOTA_RESET_PERIOD] ?: QuotaResetPeriod.MONTHLY.name
            val resetPeriod = try {
                QuotaResetPeriod.valueOf(resetPeriodStr)
            } catch (_: Exception) {
                QuotaResetPeriod.MONTHLY
            }
            val resetDay = prefs[PreferencesKeys.QUOTA_RESET_DAY] ?: 1
            val warn50 = prefs[PreferencesKeys.QUOTA_WARN_50] ?: true
            val warn75 = prefs[PreferencesKeys.QUOTA_WARN_75] ?: true
            val warn90 = prefs[PreferencesKeys.QUOTA_WARN_90] ?: true
            val warn100 = prefs[PreferencesKeys.QUOTA_WARN_100] ?: true

            QuotaSettings(
                isEnabled = isEnabled,
                limitBytes = limit,
                resetPeriod = resetPeriod,
                resetDay = resetDay,
                warnAt50 = warn50,
                warnAt75 = warn75,
                warnAt90 = warn90,
                warnAt100 = warn100
            )
        }

    suspend fun updateTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME] = theme.name
        }
    }

    suspend fun updateDataUnitFormat(format: DataUnitFormat) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DATA_UNIT] = format.name
        }
    }

    suspend fun updateDefaultPeriod(period: PeriodType) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DEFAULT_PERIOD] = period.name
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateQuotaSettings(settings: QuotaSettings) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUOTA_ENABLED] = settings.isEnabled
            prefs[PreferencesKeys.QUOTA_LIMIT_BYTES] = settings.limitBytes
            prefs[PreferencesKeys.QUOTA_RESET_PERIOD] = settings.resetPeriod.name
            prefs[PreferencesKeys.QUOTA_RESET_DAY] = settings.resetDay
            prefs[PreferencesKeys.QUOTA_WARN_50] = settings.warnAt50
            prefs[PreferencesKeys.QUOTA_WARN_75] = settings.warnAt75
            prefs[PreferencesKeys.QUOTA_WARN_90] = settings.warnAt90
            prefs[PreferencesKeys.QUOTA_WARN_100] = settings.warnAt100
        }
    }

    suspend fun resetAllSettings() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
