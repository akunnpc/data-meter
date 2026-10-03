package com.mamang.datameter.presentation.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mamang.datameter.DataMeterApplication
import com.mamang.datameter.core.network.NetworkConnectionState
import com.mamang.datameter.core.network.NetworkMonitor
import com.mamang.datameter.core.ui.UiState
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.core.utils.PermissionHelper
import com.mamang.datameter.domain.model.ChartDataFilter
import com.mamang.datameter.domain.model.DailyUsagePoint
import com.mamang.datameter.domain.model.QuotaSettings
import com.mamang.datameter.domain.model.QuotaStatus
import com.mamang.datameter.domain.model.UsageSummary
import com.mamang.datameter.domain.usecase.GetNetworkUsageUseCase
import com.mamang.datameter.domain.usecase.GetQuotaStatusUseCase
import com.mamang.datameter.domain.usecase.GetUsageChartDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardData(
    val summary: UsageSummary,
    val chartPoints: List<DailyUsagePoint>,
    val quotaStatus: QuotaStatus,
    val unitFormat: DataUnitFormat = DataUnitFormat.BINARY,
    val selectedPeriod: PeriodType = PeriodType.TODAY,
    val chartFilter: ChartDataFilter = ChartDataFilter.ALL
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as DataMeterApplication).appContainer

    private val networkMonitor: NetworkMonitor = appContainer.networkMonitor
    private val getNetworkUsageUseCase: GetNetworkUsageUseCase = appContainer.getNetworkUsageUseCase
    private val getUsageChartDataUseCase: GetUsageChartDataUseCase = appContainer.getUsageChartDataUseCase
    private val getQuotaStatusUseCase: GetQuotaStatusUseCase = appContainer.getQuotaStatusUseCase
    private val preferencesRepository = appContainer.preferencesRepository

    val connectionState: StateFlow<NetworkConnectionState> = networkMonitor.connectionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = networkMonitor.getCurrentConnectionState()
        )

    private val _hasPermission = MutableStateFlow(
        PermissionHelper.hasUsageStatsPermission(application)
    )
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(PeriodType.TODAY)
    val selectedPeriod: StateFlow<PeriodType> = _selectedPeriod.asStateFlow()

    private val _chartFilter = MutableStateFlow(ChartDataFilter.ALL)
    val chartFilter: StateFlow<ChartDataFilter> = _chartFilter.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<DashboardData>>(UiState.Loading)
    val uiState: StateFlow<UiState<DashboardData>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userSettings = preferencesRepository.userSettingsFlow.first()
            _selectedPeriod.value = userSettings.defaultPeriod
            refreshData()
        }
    }

    fun checkPermissionAndRefresh() {
        val currentHasPerm = PermissionHelper.hasUsageStatsPermission(getApplication())
        _hasPermission.value = currentHasPerm
        refreshData()
    }

    fun setPeriod(period: PeriodType) {
        _selectedPeriod.value = period
        refreshData()
    }

    fun setChartFilter(filter: ChartDataFilter) {
        _chartFilter.value = filter
        val current = _uiState.value
        if (current is UiState.Success) {
            _uiState.value = UiState.Success(current.data.copy(chartFilter = filter))
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val hasPerm = PermissionHelper.hasUsageStatsPermission(getApplication())
            _hasPermission.value = hasPerm

            if (!hasPerm) {
                _uiState.value = UiState.Empty("Akses penggunaan diperlukan untuk menampilkan statistik jaringan.")
                return@launch
            }

            try {
                val userSettings = preferencesRepository.userSettingsFlow.first()
                val quotaSettings = preferencesRepository.quotaSettingsFlow.first()

                val summaryResult = getNetworkUsageUseCase(
                    periodType = _selectedPeriod.value,
                    resetDay = quotaSettings.resetDay
                )
                val chartResult = getUsageChartDataUseCase(
                    periodType = _selectedPeriod.value,
                    resetDay = quotaSettings.resetDay
                )
                val quotaResult = getQuotaStatusUseCase(quotaSettings)

                val summary = summaryResult.getOrDefault(Pair(UsageSummary.EMPTY, "")).first
                val chartPoints = chartResult.getOrDefault(emptyList())
                val quotaStatus = quotaResult.getOrDefault(QuotaStatus())

                if (!summary.hasAnyData && chartPoints.isEmpty()) {
                    _uiState.value = UiState.Empty("Tidak ada data penggunaan untuk periode ini.")
                } else {
                    _uiState.value = UiState.Success(
                        DashboardData(
                            summary = summary,
                            chartPoints = chartPoints,
                            quotaStatus = quotaStatus,
                            unitFormat = userSettings.dataUnitFormat,
                            selectedPeriod = _selectedPeriod.value,
                            chartFilter = _chartFilter.value
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    message = e.localizedMessage ?: "Terjadi kesalahan saat memuat data statistik."
                )
            }
        }
    }
}
