package com.mamang.datameter.presentation.apps

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.mamang.datameter.core.ui.UiState
import com.mamang.datameter.core.utils.PermissionHelper
import com.mamang.datameter.domain.model.AppUsageFilter
import com.mamang.datameter.presentation.apps.components.AppUsageItem
import com.mamang.datameter.presentation.components.DataMeterTopAppBar
import com.mamang.datameter.presentation.components.EmptyStateView
import com.mamang.datameter.presentation.components.UsageAccessPermissionCard
import com.mamang.datameter.presentation.dashboard.components.PeriodSelectorRow

@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasPermission.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedAppForDetail by viewModel.selectedAppForDetail.collectAsStateWithLifecycle()
    val unitFormat by viewModel.unitFormat.collectAsStateWithLifecycle()
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
                title = stringResource(R.string.apps_title),
                connectionState = connectionState,
                actions = {
                    IconButton(
                        onClick = { viewModel.loadApps() },
                        modifier = Modifier.testTag("apps_refresh_button")
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
        ) {
            // Permission Banner
            if (!hasPermission) {
                UsageAccessPermissionCard(
                    onGrantClick = {
                        PermissionHelper.openUsageAccessSettings(context)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Search Bar
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
                            Icon(imageVector = Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("app_search_field")
            )

            // Filter Chips (Semua, Seluler, Wi-Fi)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    Pair(AppUsageFilter.ALL, stringResource(R.string.apps_filter_all)),
                    Pair(AppUsageFilter.MOBILE, stringResource(R.string.apps_filter_mobile)),
                    Pair(AppUsageFilter.WIFI, stringResource(R.string.apps_filter_wifi))
                )

                filters.forEach { (itemFilter, label) ->
                    FilterChip(
                        selected = filter == itemFilter,
                        onClick = { viewModel.setFilter(itemFilter) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("apps_filter_${itemFilter.name.lowercase()}")
                    )
                }
            }

            // Period Selector
            PeriodSelectorRow(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { viewModel.setPeriod(it) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main App List
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.testTag("apps_loading"))
                    }
                }

                is UiState.Empty -> {
                    EmptyStateView(
                        message = state.message.ifBlank { stringResource(R.string.apps_empty) },
                        onActionClick = { viewModel.loadApps() },
                        actionLabel = stringResource(R.string.refresh_btn)
                    )
                }

                is UiState.Error -> {
                    EmptyStateView(
                        message = state.message,
                        onActionClick = { viewModel.loadApps() },
                        actionLabel = stringResource(R.string.refresh_btn)
                    )
                }

                is UiState.Success -> {
                    val appList = state.data
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("apps_lazy_column"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = appList,
                            key = { it.uid }
                        ) { app ->
                            AppUsageItem(
                                app = app,
                                filter = filter,
                                unitFormat = unitFormat,
                                onClick = { viewModel.selectAppForDetail(app) }
                            )
                        }
                    }
                }
            }
        }
    }

    // App Detail Bottom Sheet
    selectedAppForDetail?.let { app ->
        AppDetailBottomSheet(
            app = app,
            unitFormat = unitFormat,
            onDismiss = { viewModel.selectAppForDetail(null) }
        )
    }
}
