package com.mamang.datameter.presentation.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.mamang.datameter.core.ui.theme.DownloadColor
import com.mamang.datameter.core.ui.theme.UploadColor
import com.mamang.datameter.core.utils.DataSizeFormatter
import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.domain.model.ChartDataFilter
import com.mamang.datameter.domain.model.DailyUsagePoint

@Composable
fun UsageChartView(
    points: List<DailyUsagePoint>,
    selectedFilter: ChartDataFilter,
    onFilterChanged: (ChartDataFilter) -> Unit,
    unitFormat: DataUnitFormat,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(points, selectedFilter) { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("usage_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = surfaceVariantColor.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Title & Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val filters = listOf(
                        Pair(ChartDataFilter.ALL, stringResource(R.string.chart_filter_all)),
                        Pair(ChartDataFilter.MOBILE, stringResource(R.string.chart_filter_mobile)),
                        Pair(ChartDataFilter.WIFI, stringResource(R.string.chart_filter_wifi))
                    )

                    filters.forEach { (filter, label) ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { onFilterChanged(filter) },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("chart_filter_${filter.name.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tooltip info when a bar is selected
            if (selectedIndex in points.indices) {
                val point = points[selectedIndex]
                val rx = when (selectedFilter) {
                    ChartDataFilter.ALL -> point.rxBytes
                    ChartDataFilter.MOBILE -> point.mobileRxBytes
                    ChartDataFilter.WIFI -> point.wifiRxBytes
                }
                val tx = when (selectedFilter) {
                    ChartDataFilter.ALL -> point.txBytes
                    ChartDataFilter.MOBILE -> point.mobileTxBytes
                    ChartDataFilter.WIFI -> point.wifiTxBytes
                }
                val total = rx + tx

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${point.dateLabel}: ${DataSizeFormatter.formatBytes(total, unitFormat)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = "↓ ${DataSizeFormatter.formatBytes(rx, unitFormat)}  ↑ ${DataSizeFormatter.formatBytes(tx, unitFormat)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onSurfaceVariantColor
                    )
                }
            } else {
                Text(
                    text = "Ketuk grafik untuk detail titik",
                    style = MaterialTheme.typography.bodySmall,
                    color = onSurfaceVariantColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Chart Area
            if (points.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.chart_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurfaceVariantColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(points, selectedFilter) {
                                detectTapGestures { offset ->
                                    val count = points.size
                                    if (count > 0) {
                                        val barWidthWithSpacing = size.width / count
                                        val index = (offset.x / barWidthWithSpacing).toInt().coerceIn(0, count - 1)
                                        selectedIndex = if (selectedIndex == index) -1 else index
                                    }
                                }
                            }
                    ) {
                        drawUsageChart(
                            points = points,
                            filter = selectedFilter,
                            selectedIndex = selectedIndex,
                            textMeasurer = textMeasurer,
                            outlineColor = outlineColor,
                            labelColor = onSurfaceVariantColor,
                            activeColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawUsageChart(
    points: List<DailyUsagePoint>,
    filter: ChartDataFilter,
    selectedIndex: Int,
    textMeasurer: TextMeasurer,
    outlineColor: Color,
    labelColor: Color,
    activeColor: Color
) {
    if (points.isEmpty()) return

    val bottomPadding = 28.dp.toPx()
    val topPadding = 12.dp.toPx()
    val chartHeight = size.height - bottomPadding - topPadding

    // Find maximum total bytes to scale
    val maxBytes = points.maxOfOrNull { point ->
        when (filter) {
            ChartDataFilter.ALL -> point.totalBytes
            ChartDataFilter.MOBILE -> point.mobileTotalBytes
            ChartDataFilter.WIFI -> point.wifiTotalBytes
        }
    }?.coerceAtLeast(1024L) ?: 1024L

    // Draw grid baseline
    val baselineY = size.height - bottomPadding
    drawLine(
        color = outlineColor,
        start = Offset(0f, baselineY),
        end = Offset(size.width, baselineY),
        strokeWidth = 1.dp.toPx()
    )

    // Midline
    drawLine(
        color = outlineColor.copy(alpha = 0.5f),
        start = Offset(0f, baselineY - chartHeight / 2),
        end = Offset(size.width, baselineY - chartHeight / 2),
        strokeWidth = 0.8.dp.toPx()
    )

    val count = points.size
    val slotWidth = size.width / count
    val barWidth = (slotWidth * 0.55f).coerceIn(4.dp.toPx(), 28.dp.toPx())

    // Step for labels to prevent overlap if many days
    val labelStep = when {
        count <= 7 -> 1
        count <= 14 -> 2
        count <= 21 -> 3
        else -> 5
    }

    points.forEachIndexed { index, point ->
        val rx = when (filter) {
            ChartDataFilter.ALL -> point.rxBytes
            ChartDataFilter.MOBILE -> point.mobileRxBytes
            ChartDataFilter.WIFI -> point.wifiRxBytes
        }
        val tx = when (filter) {
            ChartDataFilter.ALL -> point.txBytes
            ChartDataFilter.MOBILE -> point.mobileTxBytes
            ChartDataFilter.WIFI -> point.wifiTxBytes
        }
        val total = rx + tx

        val barHeight = ((total.toFloat() / maxBytes.toFloat()) * chartHeight).coerceAtLeast(2.dp.toPx())
        val rxHeight = if (total > 0) (rx.toFloat() / total.toFloat()) * barHeight else 0f
        val txHeight = barHeight - rxHeight

        val xCenter = (index * slotWidth) + (slotWidth / 2f)
        val xLeft = xCenter - (barWidth / 2f)
        val isSelected = index == selectedIndex

        // Selection background highlight pill
        if (isSelected) {
            drawRoundRect(
                color = activeColor.copy(alpha = 0.15f),
                topLeft = Offset(xLeft - 4.dp.toPx(), topPadding),
                size = Size(barWidth + 8.dp.toPx(), chartHeight + 4.dp.toPx()),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }

        // Draw stacked bar (Download on bottom, Upload on top)
        val barTopY = baselineY - barHeight
        val downloadTopY = baselineY - rxHeight

        // Bottom portion (Download - Blue)
        if (rxHeight > 0f) {
            drawRoundRect(
                color = DownloadColor.copy(alpha = if (isSelected) 1f else 0.85f),
                topLeft = Offset(xLeft, downloadTopY),
                size = Size(barWidth, rxHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        // Top portion (Upload - Orange)
        if (txHeight > 0f) {
            drawRoundRect(
                color = UploadColor.copy(alpha = if (isSelected) 1f else 0.85f),
                topLeft = Offset(xLeft, barTopY),
                size = Size(barWidth, txHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }

        // Date label below baseline
        if (index % labelStep == 0 || isSelected) {
            val textLayoutResult = textMeasurer.measure(
                text = point.dateLabel,
                style = TextStyle(
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) activeColor else labelColor
                )
            )
            val textX = (xCenter - (textLayoutResult.size.width / 2f)).coerceIn(
                0f,
                size.width - textLayoutResult.size.width
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, baselineY + 6.dp.toPx())
            )
        }
    }
}
