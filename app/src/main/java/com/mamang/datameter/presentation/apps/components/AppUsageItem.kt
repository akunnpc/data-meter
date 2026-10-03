package com.mamang.datameter.presentation.apps.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.domain.model.AppUsage
import com.mamang.datameter.domain.model.AppUsageFilter

@Composable
fun AppUsageItem(
    app: AppUsage,
    filter: AppUsageFilter,
    unitFormat: DataUnitFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        }
    }

    val displayTotal = when (filter) {
        AppUsageFilter.ALL -> app.totalBytes
        AppUsageFilter.MOBILE -> app.mobileTotalBytes
        AppUsageFilter.WIFI -> app.wifiTotalBytes
    }

    val displayRx = when (filter) {
        AppUsageFilter.ALL -> app.rxBytes
        AppUsageFilter.MOBILE -> app.mobileRxBytes
        AppUsageFilter.WIFI -> app.wifiRxBytes
    }

    val displayTx = when (filter) {
        AppUsageFilter.ALL -> app.txBytes
        AppUsageFilter.MOBILE -> app.mobileTxBytes
        AppUsageFilter.WIFI -> app.wifiTxBytes
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_usage_item_${app.uid}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            if (appIcon != null) {
                val bitmap = remember(appIcon) {
                    try {
                        appIcon.toBitmap(width = 96, height = 96).asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    DefaultAppIcon(packageName = app.packageName, isSystem = app.isSystemApp)
                }
            } else {
                DefaultAppIcon(packageName = app.packageName, isSystem = app.isSystemApp)
            }

            Spacer(modifier = Modifier.width(14.dp))

            // App Name & Package
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (app.isSystemApp) {
                        Spacer(modifier = Modifier.width(6.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("OS", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = null,
                            modifier = Modifier.padding(0.dp)
                        )
                    }
                }

                Text(
                    text = "↓ ${DataSizeFormatter.formatBytes(displayRx, unitFormat)}  ↑ ${DataSizeFormatter.formatBytes(displayTx, unitFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Total Bytes
            Text(
                text = DataSizeFormatter.formatBytes(displayTotal, unitFormat),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DefaultAppIcon(packageName: String, isSystem: Boolean) {
    val icon = when {
        packageName.contains("tethering") -> Icons.Filled.WifiTethering
        packageName.contains("removed") || packageName.contains("uid.") -> Icons.Filled.Delete
        packageName.contains("downloads") -> Icons.Filled.Download
        packageName.contains("media") -> Icons.Filled.PlayCircle
        packageName.contains("phone") -> Icons.Filled.Phone
        isSystem -> Icons.Filled.Android
        else -> Icons.Filled.Apps
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
