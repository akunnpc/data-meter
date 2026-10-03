package com.mamang.datameter.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PeriodType {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS,
    THIS_MONTH,
    CUSTOM
}

data class TimeRange(
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val periodType: PeriodType,
    val label: String
)

object DateUtils {

    fun getTimeRange(
        periodType: PeriodType,
        customStartMillis: Long? = null,
        customEndMillis: Long? = null,
        billingCycleResetDay: Int = 1
    ): TimeRange {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance(TimeZone.getDefault())

        return when (periodType) {
            PeriodType.TODAY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                TimeRange(start, now, PeriodType.TODAY, "Hari Ini")
            }
            PeriodType.LAST_7_DAYS -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                TimeRange(start, now, PeriodType.LAST_7_DAYS, "7 Hari Terakhir")
            }
            PeriodType.LAST_30_DAYS -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                TimeRange(start, now, PeriodType.LAST_30_DAYS, "30 Hari Terakhir")
            }
            PeriodType.THIS_MONTH -> {
                val (start, _) = getBillingCycleRange(now, billingCycleResetDay)
                TimeRange(start, now, PeriodType.THIS_MONTH, "Bulan Ini")
            }
            PeriodType.CUSTOM -> {
                val start = customStartMillis ?: (now - 24 * 60 * 60 * 1000L)
                val end = customEndMillis ?: now
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                val label = "${formatter.format(Date(start))} - ${formatter.format(Date(end))}"
                TimeRange(start, end, PeriodType.CUSTOM, label)
            }
        }
    }

    /**
     * Calculates the billing cycle range based on a monthly reset day.
     * e.g., if resetDay is 1, cycle is from the 1st of this month to next month's 1st.
     * If resetDay is 15 and today is the 10th, cycle is from 15th of last month to 15th of this month.
     */
    fun getBillingCycleRange(referenceTime: Long, resetDay: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.timeInMillis = referenceTime
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val boundedResetDay = resetDay.coerceIn(1, 28)

        if (currentDay >= boundedResetDay) {
            // Started in this current month on resetDay
            calendar.set(Calendar.DAY_OF_MONTH, boundedResetDay)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            // Next cycle begins next month on resetDay
            calendar.add(Calendar.MONTH, 1)
            val end = calendar.timeInMillis
            return Pair(start, end)
        } else {
            // Started in previous month on resetDay
            calendar.add(Calendar.MONTH, -1)
            val maxDayLastMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val safeResetDay = boundedResetDay.coerceAtMost(maxDayLastMonth)
            calendar.set(Calendar.DAY_OF_MONTH, safeResetDay)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val start = calendar.timeInMillis

            // Ends this month on resetDay
            calendar.timeInMillis = referenceTime
            calendar.set(Calendar.DAY_OF_MONTH, boundedResetDay)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val end = calendar.timeInMillis
            return Pair(start, end)
        }
    }

    fun formatDate(timestamp: Long, pattern: String = "dd MMM yyyy"): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatHour(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDay(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
