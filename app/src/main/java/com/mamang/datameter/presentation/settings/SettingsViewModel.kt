package com.mamang.datameter.presentation.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mamang.datameter.DataMeterApplication
import com.mamang.datameter.core.network.NetworkConnectionState
import com.mamang.datameter.core.network.NetworkMonitor
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.AppTheme
import com.mamang.datameter.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as DataMeterApplication).appContainer
    private val preferencesRepository = appContainer.preferencesRepository
    private val networkMonitor: NetworkMonitor = appContainer.networkMonitor

    val connectionState: StateFlow<NetworkConnectionState> = networkMonitor.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkMonitor.getCurrentConnectionState()
        )

    val userSettings: StateFlow<UserSettings> = preferencesRepository.userSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserSettings()
        )

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog: StateFlow<Boolean> = _showResetDialog.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesRepository.updateTheme(theme)
        }
    }

    fun setDataUnitFormat(format: DataUnitFormat) {
        viewModelScope.launch {
            preferencesRepository.updateDataUnitFormat(format)
        }
    }

    fun setDefaultPeriod(period: PeriodType) {
        viewModelScope.launch {
            preferencesRepository.updateDefaultPeriod(period)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
        }
    }

    fun openResetDialog() {
        _showResetDialog.value = true
    }

    fun closeResetDialog() {
        _showResetDialog.value = false
    }

    fun confirmResetAll() {
        viewModelScope.launch {
            _showResetDialog.value = false
            preferencesRepository.resetAllSettings()
            _snackbarMessage.emit("Pengaturan lokal telah direset.")
        }
    }
}
