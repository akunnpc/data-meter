package com.mamang.datameter.presentation.apps

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
import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.AppUsageFilter
import com.mamang.datameter.domain.usecase.GetAppUsageListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppsViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as DataMeterApplication).appContainer
    private val getAppUsageListUseCase: GetAppUsageListUseCase = appContainer.getAppUsageListUseCase
    private val preferencesRepository = appContainer.preferencesRepository
    private val networkMonitor: NetworkMonitor = appContainer.networkMonitor

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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPeriod = MutableStateFlow(PeriodType.TODAY)
    val selectedPeriod: StateFlow<PeriodType> = _selectedPeriod.asStateFlow()

    private val _filter = MutableStateFlow(AppUsageFilter.ALL)
    val filter: StateFlow<AppUsageFilter> = _filter.asStateFlow()

    private val _selectedAppForDetail = MutableStateFlow<AppUsage?>(null)
    val selectedAppForDetail: StateFlow<AppUsage?> = _selectedAppForDetail.asStateFlow()

    private val _unitFormat = MutableStateFlow(DataUnitFormat.BINARY)
    val unitFormat: StateFlow<DataUnitFormat> = _unitFormat.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<AppUsage>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<AppUsage>>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val userSettings = preferencesRepository.userSettingsFlow.first()
            _selectedPeriod.value = userSettings.defaultPeriod
            _unitFormat.value = userSettings.dataUnitFormat
            loadApps()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        loadApps()
    }

    fun setPeriod(period: PeriodType) {
        _selectedPeriod.value = period
        loadApps()
    }

    fun setFilter(filter: AppUsageFilter) {
        _filter.value = filter
        loadApps()
    }

    fun selectAppForDetail(app: AppUsage?) {
        _selectedAppForDetail.value = app
    }

    fun checkPermissionAndRefresh() {
        val currentHasPerm = PermissionHelper.hasUsageStatsPermission(getApplication())
        _hasPermission.value = currentHasPerm
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading

            val hasPerm = PermissionHelper.hasUsageStatsPermission(getApplication())
            _hasPermission.value = hasPerm

            if (!hasPerm) {
                _uiState.value = UiState.Empty("Akses penggunaan diperlukan untuk menampilkan statistik aplikasi.")
                return@launch
            }

            try {
                val quotaSettings = preferencesRepository.quotaSettingsFlow.first()
                val result = getAppUsageListUseCase(
                    periodType = _selectedPeriod.value,
                    resetDay = quotaSettings.resetDay,
                    filter = _filter.value,
                    searchQuery = _searchQuery.value
                )

                result.onSuccess { apps ->
                    if (apps.isEmpty()) {
                        _uiState.value = UiState.Empty("Tidak ada data penggunaan aplikasi untuk filter ini.")
                    } else {
                        _uiState.value = UiState.Success(apps)
                    }
                }.onFailure {
                    _uiState.value = UiState.Empty("Statistik jaringan tingkat aplikasi tidak tersedia pada perangkat ini.")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(
                    e.localizedMessage ?: "Terjadi kesalahan saat memuat daftar aplikasi."
                )
            }
        }
    }
}
