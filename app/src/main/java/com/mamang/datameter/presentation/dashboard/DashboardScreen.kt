package com.mamang.datameter.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.mamang.datameter.core.ui.UiState
import com.mamang.datameter.core.ui.theme.MobileDataColor
import com.mamang.datameter.core.ui.theme.WifiDataColor
import com.mamang.datameter.core.utils.PermissionHelper
import com.mamang.datameter.presentation.components.DataMeterTopAppBar
import com.mamang.datameter.presentation.components.EmptyStateView
import com.mamang.datameter.presentation.components.UsageAccessPermissionCard
import com.mamang.datameter.presentation.dashboard.components.PeriodSelectorRow
import com.mamang.datameter.presentation.dashboard.components.RecentActivityCard
import com.mamang.datameter.presentation.dashboard.components.UsageChartView
import com.mamang.datameter.presentation.dashboard.components.UsageSummaryCard

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val chartFilter by viewModel.chartFilter.collectAsStateWithLifecycle()
    val recentActivities by viewModel.recentActivities.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh when returning from Settings (e.g., granting Usage Access)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            DataMeterTopAppBar(
                title = stringResource(R.string.dashboard_title),
                connectionState = connectionState,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshData() },
                        modifier = Modifier.testTag("dashboard_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.refresh_btn)
                        )
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Permission Banner if not granted
            if (!hasPermission) {
                UsageAccessPermissionCard(
                    onGrantClick = {
                        PermissionHelper.openUsageAccessSettings(context)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Period Selection Chips
            PeriodSelectorRow(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { viewModel.setPeriod(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("dashboard_loading"))
                    }
                }

                is UiState.Empty -> {
                    EmptyStateView(
                        message = state.message.ifBlank { stringResource(R.string.empty_usage_data) },
                        onActionClick = { viewModel.refreshData() },
                        actionLabel = stringResource(R.string.refresh_btn)
                    )
                }

                is UiState.Error -> {
                    EmptyStateView(
                        message = state.message,
                        onActionClick = { viewModel.refreshData() },
                        actionLabel = stringResource(R.string.refresh_btn)
                    )
                }

                is UiState.Success -> {
                    val data = state.data

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Total Combined Usage Card
                        UsageSummaryCard(
                            title = stringResource(R.string.total_usage_title),
                            icon = Icons.Filled.AllInclusive,
                            iconTint = MaterialTheme.colorScheme.primary,
                            usage = data.summary.totalUsage,
                            unitFormat = data.unitFormat,
                            testTagPrefix = "card_total"
                        )

                        // Mobile Data Card (with Quota integration if enabled)
                        UsageSummaryCard(
                            title = stringResource(R.string.mobile_data_card_title),
                            icon = Icons.Filled.SignalCellularAlt,
                            iconTint = MobileDataColor,
                            usage = data.summary.mobileUsage,
                            unitFormat = data.unitFormat,
                            quotaStatus = data.quotaStatus,
                            testTagPrefix = "card_mobile"
                        )

                        // Wi-Fi Card
                        UsageSummaryCard(
                            title = stringResource(R.string.wifi_card_title),
                            icon = Icons.Filled.Wifi,
                            iconTint = WifiDataColor,
                            usage = data.summary.wifiUsage,
                            unitFormat = data.unitFormat,
                            testTagPrefix = "card_wifi"
                        )

                        // Usage Chart
                        UsageChartView(
                            points = data.chartPoints,
                            selectedFilter = chartFilter,
                            onFilterChanged = { viewModel.setChartFilter(it) },
                            unitFormat = data.unitFormat
                        )

                        // Recent Activity Section
                        RecentActivityCard(
                            recentActivities = recentActivities,
                            unitFormat = data.unitFormat,
                            onViewAllClick = onNavigateToHistory
                        )
                    }
                }
            }
        }
    }
}
