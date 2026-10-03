package com.mamang.datameter.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType
import com.mamang.datameter.domain.model.AppTheme
import com.mamang.datameter.presentation.components.DataMeterTopAppBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val showResetDialog by viewModel.showResetDialog.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            DataMeterTopAppBar(
                title = stringResource(R.string.settings_title),
                connectionState = connectionState
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Setting Card
            SettingSectionCard(
                title = stringResource(R.string.settings_theme_label),
                icon = Icons.Filled.DarkMode
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        Pair(AppTheme.SYSTEM, stringResource(R.string.theme_system)),
                        Pair(AppTheme.LIGHT, stringResource(R.string.theme_light)),
                        Pair(AppTheme.DARK, stringResource(R.string.theme_dark))
                    )
                    themes.forEach { (theme, label) ->
                        FilterChip(
                            selected = userSettings.theme == theme,
                            onClick = { viewModel.setTheme(theme) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("theme_chip_${theme.name.lowercase()}")
                        )
                    }
                }
            }

            // Data Unit Format Card
            SettingSectionCard(
                title = stringResource(R.string.settings_unit_label),
                icon = Icons.Filled.FormatListNumbered
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val units = listOf(
                        Pair(DataUnitFormat.BINARY, stringResource(R.string.unit_binary)),
                        Pair(DataUnitFormat.DECIMAL, stringResource(R.string.unit_decimal))
                    )
                    units.forEach { (unit, label) ->
                        FilterChip(
                            selected = userSettings.dataUnitFormat == unit,
                            onClick = { viewModel.setDataUnitFormat(unit) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("unit_chip_${unit.name.lowercase()}")
                        )
                    }
                }
            }

            // Default Startup Period Card
            SettingSectionCard(
                title = stringResource(R.string.settings_default_period_label),
                icon = Icons.Filled.Schedule
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val periods = listOf(
                        Pair(PeriodType.TODAY, stringResource(R.string.period_today)),
                        Pair(PeriodType.LAST_7_DAYS, stringResource(R.string.period_last_7_days)),
                        Pair(PeriodType.THIS_MONTH, stringResource(R.string.period_this_month))
                    )
                    periods.forEach { (period, label) ->
                        FilterChip(
                            selected = userSettings.defaultPeriod == period,
                            onClick = { viewModel.setDefaultPeriod(period) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("default_period_${period.name.lowercase()}")
                        )
                    }
                }
            }

            // Notifications Toggle Card
            SettingSectionCard(
                title = "Notifikasi & Peringatan",
                icon = Icons.Filled.Notifications
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peringatan Kuota Data",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Kirim notifikasi lokal saat penggunaan data mencapai batas yang Anda tentukan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = userSettings.notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        modifier = Modifier.testTag("notifications_switch")
                    )
                }
            }

            // About DataMeter & Privacy Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(R.string.about_version),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.about_privacy_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = stringResource(R.string.about_privacy_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reset Local Settings Button
            OutlinedButton(
                onClick = { viewModel.openResetDialog() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reset_settings_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(imageVector = Icons.Filled.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_reset_label),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Confirmation Dialog for Resetting Local Settings
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.closeResetDialog() },
            title = {
                Text(
                    text = stringResource(R.string.settings_reset_confirm_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.settings_reset_confirm_message))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmResetAll() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_reset_button")
                ) {
                    Text(stringResource(R.string.confirm_reset))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.closeResetDialog() },
                    modifier = Modifier.testTag("cancel_reset_button")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
            modifier = Modifier.testTag("reset_confirm_dialog")
        )
    }
}

@Composable
private fun SettingSectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}
