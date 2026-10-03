package com.mamang.datameter.presentation.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.mamang.datameter.core.ui.theme.DownloadColor
import com.mamang.datameter.core.ui.theme.QuotaExceededColor
import com.mamang.datameter.core.ui.theme.QuotaSafeColor
import com.mamang.datameter.core.ui.theme.QuotaWarningColor
import com.mamang.datameter.core.ui.theme.UploadColor
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.domain.model.NetworkUsage
import com.mamang.datameter.domain.model.QuotaStatus

@Composable
fun UsageSummaryCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    usage: NetworkUsage,
    unitFormat: DataUnitFormat,
    modifier: Modifier = Modifier,
    quotaStatus: QuotaStatus? = null,
    testTagPrefix: String = "usage_card"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagPrefix),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = iconTint
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = DataSizeFormatter.formatBytes(usage.totalBytes, unitFormat),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("${testTagPrefix}_total_text")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Download & Upload Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                UsageMetricItem(
                    label = stringResource(R.string.download_label),
                    value = DataSizeFormatter.formatBytes(usage.rxBytes, unitFormat),
                    icon = Icons.Filled.ArrowDownward,
                    color = DownloadColor,
                    testTag = "${testTagPrefix}_download"
                )

                UsageMetricItem(
                    label = stringResource(R.string.upload_label),
                    value = DataSizeFormatter.formatBytes(usage.txBytes, unitFormat),
                    icon = Icons.Filled.ArrowUpward,
                    color = UploadColor,
                    testTag = "${testTagPrefix}_upload"
                )
            }

            // Quota section if available and enabled
            if (quotaStatus != null && quotaStatus.isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))

                val progress = (quotaStatus.percentage / 100f).coerceIn(0f, 1f)
                val progressColor = when {
                    quotaStatus.isExceeded || quotaStatus.percentage >= 90f -> QuotaExceededColor
                    quotaStatus.percentage >= 75f -> QuotaWarningColor
                    else -> QuotaSafeColor
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                        .testTag("${testTagPrefix}_quota_section")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stringResource(R.string.quota_title)}: ${DataSizeFormatter.formatBytes(quotaStatus.limitBytes, unitFormat)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format("%.1f%%", quotaStatus.percentage),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = progressColor
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stringResource(R.string.used_label)}: ${DataSizeFormatter.formatBytes(quotaStatus.usedBytes, unitFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${stringResource(R.string.remaining_label)}: ${DataSizeFormatter.formatBytes(quotaStatus.remainingBytes, unitFormat)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageMetricItem(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
