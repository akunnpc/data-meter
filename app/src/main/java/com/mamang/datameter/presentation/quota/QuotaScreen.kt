package com.mamang.datameter.presentation.quota

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.mamang.datameter.core.ui.theme.QuotaExceededColor
import com.mamang.datameter.core.ui.theme.QuotaSafeColor
import com.mamang.datameter.core.ui.theme.QuotaWarningColor
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.domain.model.QuotaResetPeriod
import com.mamang.datameter.presentation.components.DataMeterTopAppBar
import kotlinx.coroutines.flow.collectLatest

@Composable
fun QuotaScreen(
    viewModel: QuotaViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val quotaSettings by viewModel.quotaSettings.collectAsStateWithLifecycle()
    val quotaStatus by viewModel.quotaStatus.collectAsStateWithLifecycle()
    val unitFormat by viewModel.unitFormat.collectAsStateWithLifecycle()
    val amountInput by viewModel.amountInput.collectAsStateWithLifecycle()
    val selectedUnit by viewModel.selectedUnit.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            DataMeterTopAppBar(
                title = stringResource(R.string.quota_title),
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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Toggle Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quota_master_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.quota_enabled_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pantau batas pemakaian data seluler dan dapatkan notifikasi peringatan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = quotaSettings.isEnabled,
                        onCheckedChange = { viewModel.onEnabledChanged(it) },
                        modifier = Modifier.testTag("quota_switch")
                    )
                }
            }

            // Quota Status & Progress Card (Visible if enabled)
            if (quotaSettings.isEnabled) {
                val progress = (quotaStatus.percentage / 100f).coerceIn(0f, 1f)
                val progressColor = when {
                    quotaStatus.isExceeded || quotaStatus.percentage >= 90f -> QuotaExceededColor
                    quotaStatus.percentage >= 75f -> QuotaWarningColor
                    else -> QuotaSafeColor
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quota_status_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Penggunaan Siklus (${quotaStatus.billingCycleLabel})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.1f%%", quotaStatus.percentage),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = progressColor,
                                modifier = Modifier.testTag("quota_percentage_text")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.used_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DataSizeFormatter.formatBytes(quotaStatus.usedBytes, unitFormat),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.remaining_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = DataSizeFormatter.formatBytes(quotaStatus.remainingBytes, unitFormat),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (quotaStatus.isExceeded) QuotaExceededColor else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Quota Configuration Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quota_form_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Konfigurasi Kuota",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Amount + Unit Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { viewModel.onAmountInputChanged(it) },
                            label = { Text(stringResource(R.string.quota_limit_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quota_amount_input")
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("GB", "MB").forEach { unit ->
                                FilterChip(
                                    selected = selectedUnit == unit,
                                    onClick = { viewModel.onUnitChanged(unit) },
                                    label = { Text(unit) },
                                    modifier = Modifier.testTag("unit_chip_$unit")
                                )
                            }
                        }
                    }

                    // Reset Period
                    Column {
                        Text(
                            text = stringResource(R.string.quota_reset_period_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val periods = listOf(
                                Pair(QuotaResetPeriod.MONTHLY, "Bulanan"),
                                Pair(QuotaResetPeriod.WEEKLY, "Mingguan"),
                                Pair(QuotaResetPeriod.DAILY, "Harian")
                            )
                            periods.forEach { (period, label) ->
                                FilterChip(
                                    selected = quotaSettings.resetPeriod == period,
                                    onClick = { viewModel.onResetPeriodChanged(period) },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("reset_period_${period.name.lowercase()}")
                                )
                            }
                        }
                    }

                    // Monthly Reset Day Slider (If Monthly)
                    if (quotaSettings.resetPeriod == QuotaResetPeriod.MONTHLY) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.quota_reset_day_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tanggal ${quotaSettings.resetDay}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = quotaSettings.resetDay.toFloat(),
                                onValueChange = { viewModel.onResetDayChanged(it.toInt()) },
                                valueRange = 1f..28f,
                                steps = 26,
                                modifier = Modifier.testTag("reset_day_slider")
                            )
                        }
                    }
                }
            }

            // Warning Thresholds Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quota_thresholds_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quota_warning_thresholds),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ThresholdSwitchRow(
                        title = stringResource(R.string.quota_threshold_50),
                        checked = quotaSettings.warnAt50,
                        onCheckedChange = { viewModel.onThresholdToggled(50, it) },
                        testTag = "threshold_50_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ThresholdSwitchRow(
                        title = stringResource(R.string.quota_threshold_75),
                        checked = quotaSettings.warnAt75,
                        onCheckedChange = { viewModel.onThresholdToggled(75, it) },
                        testTag = "threshold_75_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ThresholdSwitchRow(
                        title = stringResource(R.string.quota_threshold_90),
                        checked = quotaSettings.warnAt90,
                        onCheckedChange = { viewModel.onThresholdToggled(90, it) },
                        testTag = "threshold_90_switch"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ThresholdSwitchRow(
                        title = stringResource(R.string.quota_threshold_100),
                        checked = quotaSettings.warnAt100,
                        onCheckedChange = { viewModel.onThresholdToggled(100, it) },
                        testTag = "threshold_100_switch"
                    )
                }
            }

            // Save Settings Button
            Button(
                onClick = { viewModel.saveQuotaSettings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_quota_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.save_quota_settings),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ThresholdSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
