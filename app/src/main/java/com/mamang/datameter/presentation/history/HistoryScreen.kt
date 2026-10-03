package com.mamang.datameter.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.mamang.datameter.core.ui.UiState
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.core.utils.PermissionHelper
import com.mamang.datameter.domain.model.NetworkActivityType
import com.mamang.datameter.presentation.components.DataMeterTopAppBar
import com.mamang.datameter.presentation.components.EmptyStateView
import com.mamang.datameter.presentation.components.UsageAccessPermissionCard
import com.mamang.datameter.presentation.history.components.HistoryItemCard
import com.mamang.datameter.presentation.history.components.HistorySummaryCard

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val selectedNetworkType by viewModel.selectedNetworkType.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isAscending by viewModel.isAscending.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                title = stringResource(R.string.history_title),
                connectionState = connectionState,
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshSnapshot() },
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("history_refresh_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.refresh_btn)
                            )
                        }
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
        ) {
            // Permission warning if not granted
            if (!hasPermission) {
                UsageAccessPermissionCard(
                    onGrantClick = {
                        PermissionHelper.openUsageAccessSettings(context)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Period Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("history_period_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf(
                    Pair(PeriodType.TODAY, stringResource(R.string.period_today)),
                    Pair(PeriodType.LAST_7_DAYS, stringResource(R.string.period_last_7_days)),
                    Pair(PeriodType.LAST_30_DAYS, stringResource(R.string.period_last_30_days)),
                    Pair(PeriodType.THIS_MONTH, stringResource(R.string.period_this_month))
                )

                periods.forEach { (period, label) ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { viewModel.setPeriod(period) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("history_period_${period.name.lowercase()}")
                    )
                }
            }

            // Network Type Filters & Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val networkFilters = listOf(
                        Pair(null, stringResource(R.string.history_filter_all)),
                        Pair(NetworkActivityType.WIFI, "Wi-Fi"),
                        Pair(NetworkActivityType.CELLULAR, "Seluler")
                    )

                    networkFilters.forEach { (netType, label) ->
                        FilterChip(
                            selected = selectedNetworkType == netType,
                            onClick = { viewModel.setNetworkType(netType) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("history_net_${label.lowercase()}")
                        )
                    }
                }

                // Sort toggle (Terbaru / Terlama)
                OutlinedButton(
                    onClick = { viewModel.toggleSortOrder() },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("history_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAscending) stringResource(R.string.history_sort_oldest) else stringResource(R.string.history_sort_newest),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            // Search input field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(stringResource(R.string.apps_search_hint)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Hapus")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("history_search_input")
            )

            // Content Area
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("history_loading"))
                    }
                }

                is UiState.Empty -> {
                    EmptyStateView(
                        message = stringResource(R.string.history_empty),
                        onActionClick = { viewModel.refreshSnapshot() },
                        actionLabel = stringResource(R.string.refresh_btn)
                    )
                }

                is UiState.Error -> {
                    EmptyStateView(
                        message = state.message,
                        onActionClick = { viewModel.refreshSnapshot() },
                        actionLabel = stringResource(R.string.refresh_btn)
                    )
                }

                is UiState.Success -> {
                    val data = state.data
                    val activities = data.activities

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("history_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary header card
                        item(key = "history_summary_card") {
                            HistorySummaryCard(
                                totalBytes = data.totalBytes,
                                rxBytes = data.totalRxBytes,
                                txBytes = data.totalTxBytes,
                                eventCount = data.activityCount,
                                unitFormat = data.unitFormat,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        // Chronological activity list with date group headers
                        itemsIndexed(
                            items = activities,
                            key = { _, item -> item.id }
                        ) { index, activity ->
                            val showDateHeader = index == 0 || activities[index - 1].formattedDate != activity.formattedDate
                            if (showDateHeader) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = activity.formattedDate,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HistoryItemCard(
                                activity = activity,
                                unitFormat = data.unitFormat
                            )
                        }
                    }
                }
            }
        }
    }
}
