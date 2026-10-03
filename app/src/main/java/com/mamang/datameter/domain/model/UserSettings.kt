package com.mamang.datameter.domain.model

import com.mamang.datameter.core.utils.DataUnitFormat
import com.mamang.datameter.core.utils.PeriodType

enum class AppTheme {
    SYSTEM,
    LIGHT,
    DARK
}

data class UserSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dataUnitFormat: DataUnitFormat = DataUnitFormat.BINARY,
    val defaultPeriod: PeriodType = PeriodType.TODAY,
    val notificationsEnabled: Boolean = true
)
