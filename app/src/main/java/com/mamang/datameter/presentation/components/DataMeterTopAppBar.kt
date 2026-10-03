package com.mamang.datameter.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.mamang.datameter.core.network.NetworkConnectionState
import com.mamang.datameter.core.network.NetworkType
import com.mamang.datameter.core.ui.theme.MobileDataColor
import com.mamang.datameter.core.ui.theme.WifiDataColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataMeterTopAppBar(
    title: String,
    connectionState: NetworkConnectionState,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            NetworkStatusBadge(connectionState = connectionState)
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.testTag("datameter_top_app_bar")
    )
}

@Composable
fun NetworkStatusBadge(
    connectionState: NetworkConnectionState,
    modifier: Modifier = Modifier
) {
    val (label, icon, dotColor) = when (connectionState.type) {
        NetworkType.WIFI -> Triple(
            stringResource(R.string.network_status_wifi),
            Icons.Filled.Wifi,
            WifiDataColor
        )
        NetworkType.CELLULAR -> Triple(
            stringResource(R.string.network_status_cellular),
            Icons.Filled.SignalCellularAlt,
            MobileDataColor
        )
        NetworkType.ETHERNET -> Triple(
            "Ethernet",
            Icons.Filled.Wifi,
            WifiDataColor
        )
        NetworkType.DISCONNECTED -> Triple(
            stringResource(R.string.network_status_disconnected),
            Icons.Filled.WifiOff,
            Color.Gray
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(end = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("network_status_badge")
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}
