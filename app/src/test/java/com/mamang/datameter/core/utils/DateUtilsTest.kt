package com.mamang.datameter.core.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DateUtilsTest {

    @Test
    fun getTimeRange_today_startsAtMidnight() {
        val range = DateUtils.getTimeRange(PeriodType.TODAY)
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = range.startTimeMillis

        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertTrue(range.endTimeMillis >= range.startTimeMillis)
    }

    @Test
    fun getBillingCycleRange_returnsValidInterval() {
        val now = System.currentTimeMillis()
        val (start, end) = DateUtils.getBillingCycleRange(now, resetDay = 1)
        assertTrue("Start must be before or equal to now", start <= now)
        assertTrue("End must be after now", end > now)
        assertTrue("Interval must be positive", end > start)
    }
}
