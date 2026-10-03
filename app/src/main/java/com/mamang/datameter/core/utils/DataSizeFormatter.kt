package com.mamang.datameter.core.utils

import java.util.Locale

enum class DataUnitFormat {
    BINARY, // 1024 bytes = 1 KiB
    DECIMAL // 1000 bytes = 1 KB
}

data class FormattedDataSize(
    val value: String,
    val unit: String,
    val fullText: String
)

object DataSizeFormatter {

    fun format(bytes: Long, format: DataUnitFormat = DataUnitFormat.BINARY): FormattedDataSize {
        if (bytes <= 0L) {
            return FormattedDataSize(value = "0", unit = "B", fullText = "0 B")
        }

        val divisor = if (format == DataUnitFormat.BINARY) 1024.0 else 1000.0
        val units = if (format == DataUnitFormat.BINARY) {
            arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        } else {
            arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        }

        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= divisor && unitIndex < units.size - 1) {
            size /= divisor
            unitIndex++
        }

        val formattedValue = when {
            unitIndex == 0 -> String.format(Locale.getDefault(), "%.0f", size)
            size >= 100 -> String.format(Locale.getDefault(), "%.1f", size)
            else -> String.format(Locale.getDefault(), "%.2f", size)
        }

        val unit = units[unitIndex]
        return FormattedDataSize(
            value = formattedValue,
            unit = unit,
            fullText = "$formattedValue $unit"
        )
    }

    fun formatBytes(bytes: Long, format: DataUnitFormat = DataUnitFormat.BINARY): String {
        return format(bytes, format).fullText
    }

    fun gigabytesToBytes(gb: Double, format: DataUnitFormat = DataUnitFormat.BINARY): Long {
        val multiplier = if (format == DataUnitFormat.BINARY) 1024.0 * 1024.0 * 1024.0 else 1000.0 * 1000.0 * 1000.0
        return (gb * multiplier).toLong()
    }

    fun megabytesToBytes(mb: Double, format: DataUnitFormat = DataUnitFormat.BINARY): Long {
        val multiplier = if (format == DataUnitFormat.BINARY) 1024.0 * 1024.0 else 1000.0 * 1000.0
        return (mb * multiplier).toLong()
    }

    fun bytesToGigabytes(bytes: Long, format: DataUnitFormat = DataUnitFormat.BINARY): Double {
        val multiplier = if (format == DataUnitFormat.BINARY) 1024.0 * 1024.0 * 1024.0 else 1000.0 * 1000.0 * 1000.0
        return bytes / multiplier
    }
}
