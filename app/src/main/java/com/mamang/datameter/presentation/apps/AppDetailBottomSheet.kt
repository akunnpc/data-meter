package com.mamang.datameter.presentation.apps

import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.mamang.datameter.core.ui.theme.DownloadColor
import com.mamang.datameter.core.ui.theme.MobileDataColor
import com.mamang.datameter.core.ui.theme.UploadColor
import com.mamang.datameter.core.ui.theme.WifiDataColor
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.domain.model.AppUsage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: AppUsage,
    unitFormat: DataUnitFormat,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    val appIcon: Drawable? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName)
        } catch (_: Exception) {
            null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("app_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header
            if (appIcon != null) {
                val bitmap = remember(appIcon) {
                    try {
                        appIcon.toBitmap(width = 144, height = 144).asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    DefaultAppDetailIcon(packageName = app.packageName, isSystem = app.isSystemApp)
                }
            } else {
                DefaultAppDetailIcon(packageName = app.packageName, isSystem = app.isSystemApp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Total Usage Highlight Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.total_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = DataSizeFormatter.formatBytes(app.totalBytes, unitFormat),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Network Breakdowns: Mobile vs Wi-Fi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mobile Data breakdown
                NetworkBreakdownCard(
                    title = stringResource(R.string.mobile_data_card_title),
                    icon = Icons.Filled.SignalCellularAlt,
                    iconTint = MobileDataColor,
                    total = app.mobileTotalBytes,
                    rx = app.mobileRxBytes,
                    tx = app.mobileTxBytes,
                    unitFormat = unitFormat,
                    modifier = Modifier.weight(1f)
                )

                // Wi-Fi breakdown
                NetworkBreakdownCard(
                    title = stringResource(R.string.wifi_card_title),
                    icon = Icons.Filled.Wifi,
                    iconTint = WifiDataColor,
                    total = app.wifiTotalBytes,
                    rx = app.wifiRxBytes,
                    tx = app.wifiTxBytes,
                    unitFormat = unitFormat,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Button to open system app info if valid package
            if (app.packageName.contains(".")) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.packageName, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_system_app_info_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Info Aplikasi di Pengaturan")
                }
            }
        }
    }
}

@Composable
private fun NetworkBreakdownCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    total: Long,
    rx: Long,
    tx: Long,
    unitFormat: DataUnitFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = iconTint
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = DataSizeFormatter.formatBytes(total, unitFormat),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = DownloadColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = DataSizeFormatter.formatBytes(rx, unitFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = UploadColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = DataSizeFormatter.formatBytes(tx, unitFormat),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DefaultAppDetailIcon(packageName: String, isSystem: Boolean) {
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
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
