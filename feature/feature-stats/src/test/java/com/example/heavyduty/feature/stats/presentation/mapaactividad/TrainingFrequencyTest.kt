package com.example.heavyduty.feature.stats.presentation.mapaactividad

import com.example.heavyduty.feature.stats.domain.usecase.DayTotals
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingFrequencyTest {

    // Saturday 10 October 2026, mid-afternoon.
    private val now = at(2026, Calendar.OCTOBER, 10, hour = 16)

    private fun at(year: Int, month: Int, day: Int, hour: Int = 0): Long = Calendar.getInstance().apply {
        clear()
        set(year, month, day, hour, 0)
    }.timeInMillis

    private fun day(month: Int, day: Int, year: Int = 2026) = at(year, month, day)

    private fun dayOfMonth(millis: Long) = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.DAY_OF_MONTH)

    private val history = mapOf(
        day(Calendar.OCTOBER, 7) to DayTotals(sessions = 1, minutes = 60f),
        day(Calendar.OCTOBER, 9) to DayTotals(sessions = 1, minutes = 2100f), // a workout left running
        day(Calendar.OCTOBER, 10) to DayTotals(sessions = 2, minutes = 90f),
        day(Calendar.SEPTEMBER, 30) to DayTotals(sessions = 1, minutes = 45f),
        day(Calendar.MARCH, 3) to DayTotals(sessions = 1, minutes = 50f)
    )

    @Test
    fun `the week runs Monday to Sunday around today`() {
        val panel = buildFrequencyPanel(FrequencyView.WEEK, offset = 0, now = now, dailyTotals = history)
        val days = (panel.content as FrequencyContent.Week).days

        assertEquals((5..11).toList(), days.map { it.dayOfMonth })
        assertTrue(days[5].isToday)
        assertTrue(days[6].isFuture)
        assertEquals(4, panel.sessions)
        assertEquals(3, panel.trainedDays)
        assertEquals(2250f, panel.minutes, 0.01f)
    }

    @Test
    fun `a short day still shows beside a very long one, and a rest day doesn't`() {
        val days = (buildFrequencyPanel(FrequencyView.WEEK, 0, now, history).content as FrequencyContent.Week).days

        assertTrue(days.single { it.dayOfMonth == 7 }.intensity >= MIN_TRAINED_INTENSITY)
        assertEquals(1f, days.single { it.dayOfMonth == 9 }.intensity, 0.001f)
        assertEquals(0f, days.single { it.dayOfMonth == 8 }.intensity, 0f)
    }

    @Test
    fun `the month is a Monday-first calendar padded to whole weeks`() {
        val panel = buildFrequencyPanel(FrequencyView.MONTH, 0, now, history)
        val weeks = (panel.content as FrequencyContent.Month).weeks

        // 1 October 2026 is a Thursday: three blank cells first, 31 days, five rows of seven.
        assertEquals(5, weeks.size)
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(listOf(null, null, null), weeks.first().take(3))
        assertEquals(1, weeks.first()[3]?.dayOfMonth)
        assertEquals(31, weeks.flatten().filterNotNull().size)
        assertEquals(3, panel.trainedDays) // 30 September belongs to the previous month
    }

    @Test
    fun `going back a month shows September and its workouts`() {
        val panel = buildFrequencyPanel(FrequencyView.MONTH, -1, now, history)

        assertEquals(day(Calendar.SEPTEMBER, 1), panel.periodStart)
        assertEquals(day(Calendar.OCTOBER, 1), panel.periodEnd)
        assertEquals(1, panel.sessions)
        assertTrue(panel.canGoForward)
        assertTrue("March is still further back", panel.canGoBack)
    }

    @Test
    fun `there is nothing to go back to before the first workout, or forward from today`() {
        val current = buildFrequencyPanel(FrequencyView.MONTH, 0, now, history)
        val march = buildFrequencyPanel(FrequencyView.MONTH, -7, now, history)

        assertFalse(current.canGoForward)
        assertEquals(1, march.sessions)
        assertFalse(march.canGoBack)
        assertFalse(buildFrequencyPanel(FrequencyView.WEEK, 0, now, emptyMap()).canGoBack)
    }

    @Test
    fun `the year counts trained days per month`() {
        val panel = buildFrequencyPanel(FrequencyView.YEAR, 0, now, history)
        val months = (panel.content as FrequencyContent.Year).months

        assertEquals(12, months.size)
        assertEquals(1, months[Calendar.MARCH].trainedDays)
        assertEquals(1, months[Calendar.SEPTEMBER].trainedDays)
        assertEquals(3, months[Calendar.OCTOBER].trainedDays)
        assertEquals(4, months[Calendar.OCTOBER].sessions)
        assertTrue(months[Calendar.OCTOBER].isCurrent)
        assertTrue(months[Calendar.NOVEMBER].isFuture)
        assertEquals(5, panel.trainedDays)
    }

    @Test
    fun `a week that spans two months keeps its days in order`() {
        // Monday 28 September - Sunday 4 October.
        val panel = buildFrequencyPanel(FrequencyView.WEEK, -1, now, history)
        val days = (panel.content as FrequencyContent.Week).days

        assertEquals(listOf(28, 29, 30, 1, 2, 3, 4), days.map { it.dayOfMonth })
        assertEquals(28, dayOfMonth(panel.periodStart))
        assertEquals(1, panel.sessions)
        assertNull(days.firstOrNull { it.isToday })
    }
}
