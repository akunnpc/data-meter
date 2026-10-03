package com.mamang.datameter.presentation.quota

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mamang.datameter.DataMeterApplication
import com.mamang.datameter.core.network.NetworkConnectionState
import com.mamang.datameter.core.network.NetworkMonitor
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.domain.model.QuotaResetPeriod
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.QuotaStatus
import com.mamang.datameter.domain.usecase.GetQuotaStatusUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuotaViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as DataMeterApplication).appContainer
    private val preferencesRepository = appContainer.preferencesRepository
    private val getQuotaStatusUseCase: GetQuotaStatusUseCase = appContainer.getQuotaStatusUseCase
    private val networkMonitor: NetworkMonitor = appContainer.networkMonitor

    val connectionState: StateFlow<NetworkConnectionState> = networkMonitor.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkMonitor.getCurrentConnectionState()
        )

    private val _quotaSettings = MutableStateFlow(QuotaSettings())
    val quotaSettings: StateFlow<QuotaSettings> = _quotaSettings.asStateFlow()

    private val _quotaStatus = MutableStateFlow(QuotaStatus())
    val quotaStatus: StateFlow<QuotaStatus> = _quotaStatus.asStateFlow()

    private val _unitFormat = MutableStateFlow(DataUnitFormat.BINARY)
    val unitFormat: StateFlow<DataUnitFormat> = _unitFormat.asStateFlow()

    // Form editing states
    val amountInput = MutableStateFlow("10")
    val selectedUnit = MutableStateFlow("GB") // "GB" or "MB"

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            val userSettings = preferencesRepository.userSettingsFlow.first()
            _unitFormat.value = userSettings.dataUnitFormat

            preferencesRepository.quotaSettingsFlow.collect { settings ->
                _quotaSettings.value = settings
                // Initialize input field from stored bytes
                val gb = DataSizeFormatter.bytesToGigabytes(settings.limitBytes, userSettings.dataUnitFormat)
                if (gb >= 1.0) {
                    amountInput.value = String.format("%.0f", gb)
                    selectedUnit.value = "GB"
                } else {
                    val mb = (settings.limitBytes / (1024.0 * 1024.0))
                    amountInput.value = String.format("%.0f", mb)
                    selectedUnit.value = "MB"
                }

                refreshQuotaStatus(settings)
            }
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        _quotaSettings.value = _quotaSettings.value.copy(isEnabled = enabled)
    }

    fun onAmountInputChanged(value: String) {
        amountInput.value = value.filter { it.isDigit() || it == '.' }
    }

    fun onUnitChanged(unit: String) {
        selectedUnit.value = unit
    }

    fun onResetPeriodChanged(period: QuotaResetPeriod) {
        _quotaSettings.value = _quotaSettings.value.copy(resetPeriod = period)
    }

    fun onResetDayChanged(day: Int) {
        _quotaSettings.value = _quotaSettings.value.copy(resetDay = day.coerceIn(1, 28))
    }

    fun onThresholdToggled(threshold: Int, enabled: Boolean) {
        _quotaSettings.value = when (threshold) {
            50 -> _quotaSettings.value.copy(warnAt50 = enabled)
            75 -> _quotaSettings.value.copy(warnAt75 = enabled)
            90 -> _quotaSettings.value.copy(warnAt90 = enabled)
            100 -> _quotaSettings.value.copy(warnAt100 = enabled)
            else -> _quotaSettings.value
        }
    }

    fun saveQuotaSettings() {
        viewModelScope.launch {
            val amount = amountInput.value.toDoubleOrNull() ?: 10.0
            val limitBytes = if (selectedUnit.value == "GB") {
                DataSizeFormatter.gigabytesToBytes(amount, _unitFormat.value)
            } else {
                DataSizeFormatter.megabytesToBytes(amount, _unitFormat.value)
            }

            val updated = _quotaSettings.value.copy(limitBytes = limitBytes)
            preferencesRepository.updateQuotaSettings(updated)
            refreshQuotaStatus(updated)
            _snackbarMessage.emit("Pengaturan kuota berhasil disimpan")
        }
    }

    private suspend fun refreshQuotaStatus(settings: QuotaSettings) {
        val statusResult = getQuotaStatusUseCase(settings)
        _quotaStatus.value = statusResult.getOrDefault(QuotaStatus())
    }
}
