package com.mamang.datameter.presentation.history

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
import com.mamang.datameter.domain.model.NetworkActivity
import com.mamang.datameter.domain.model.NetworkActivityType
import com.mamang.datameter.domain.usecase.GetActivityHistoryUseCase
import com.mamang.datameter.domain.usecase.RecordNetworkActivitySnapshotUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiData(
    val activities: List<NetworkActivity>,
    val totalRxBytes: Long,
    val totalTxBytes: Long,
    val totalBytes: Long,
    val activityCount: Int,
    val unitFormat: DataUnitFormat
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val appContainer = (application as DataMeterApplication).appContainer
    private val getActivityHistoryUseCase: GetActivityHistoryUseCase = appContainer.getActivityHistoryUseCase
    private val recordNetworkActivitySnapshotUseCase: RecordNetworkActivitySnapshotUseCase = appContainer.recordNetworkActivitySnapshotUseCase
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

    private val _selectedPeriod = MutableStateFlow(PeriodType.TODAY)
    val selectedPeriod: StateFlow<PeriodType> = _selectedPeriod.asStateFlow()

    private val _selectedNetworkType = MutableStateFlow<NetworkActivityType?>(null)
    val selectedNetworkType: StateFlow<NetworkActivityType?> = _selectedNetworkType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isAscending = MutableStateFlow(false) // Default false = newest first
    val isAscending: StateFlow<Boolean> = _isAscending.asStateFlow()

    private val _unitFormat = MutableStateFlow(DataUnitFormat.BINARY)
    val unitFormat: StateFlow<DataUnitFormat> = _unitFormat.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Combining filter parameters into activity flow
    val uiState: StateFlow<UiState<HistoryUiData>> = combine(
        _selectedPeriod,
        _selectedNetworkType,
        _searchQuery,
        _isAscending
    ) { period, networkType, query, isAsc ->
        FilterParams(period, networkType, query, isAsc)
    }.flatMapLatest { params ->
        getActivityHistoryUseCase(
            periodType = params.period,
            networkType = params.networkType,
            searchQuery = params.query,
            isAscending = params.isAsc
        )
    }.combine(_unitFormat) { activities, format ->
        if (activities.isEmpty()) {
            UiState.Empty()
        } else {
            val totalRx = activities.sumOf { it.rxBytes }
            val totalTx = activities.sumOf { it.txBytes }
            val total = totalRx + totalTx
            UiState.Success(
                HistoryUiData(
                    activities = activities,
                    totalRxBytes = totalRx,
                    totalTxBytes = totalTx,
                    totalBytes = total,
                    activityCount = activities.size,
                    unitFormat = format
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState.Loading
    )

    init {
        viewModelScope.launch {
            val userSettings = preferencesRepository.userSettingsFlow.first()
            _unitFormat.value = userSettings.dataUnitFormat
            refreshSnapshot()
        }
    }

    fun setPeriod(period: PeriodType) {
        _selectedPeriod.value = period
    }

    fun setNetworkType(networkType: NetworkActivityType?) {
        _selectedNetworkType.value = networkType
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSortOrder() {
        _isAscending.value = !_isAscending.value
    }

    fun checkPermissionAndRefresh() {
        _hasPermission.value = PermissionHelper.hasUsageStatsPermission(getApplication())
        refreshSnapshot()
    }

    fun refreshSnapshot() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                recordNetworkActivitySnapshotUseCase()
            } catch (_: Exception) {
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private data class FilterParams(
        val period: PeriodType,
        val networkType: NetworkActivityType?,
        val query: String,
        val isAsc: Boolean
    )
}
