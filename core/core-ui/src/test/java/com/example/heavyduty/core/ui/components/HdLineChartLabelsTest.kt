package com.example.heavyduty.core.ui.components

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class HdLineChartLabelsTest {

    private val es = Locale.Builder().setLanguage("es").setRegion("ES").build()

    private fun texts(labels: List<Pair<*, String>>) = labels.map { it.second }

    @Test
    fun `a narrow weight range gets a decimal instead of repeating the same number`() {
        // What the dashboard showed: 79.6 to 80.4 kg drew "80, 80, 80".
        assertEquals(listOf("80.4", "80.0", "79.6"), texts(yAxisLabels(low = 79.6f, high = 80.4f, formatter = null)))
    }

    @Test
    fun `a wide range keeps whole numbers`() {
        assertEquals(listOf("90", "85", "80"), texts(yAxisLabels(low = 80f, high = 90f, formatter = null)))
    }

    @Test
    fun `decimals use a point in every language, like the rest of the app`() {
        val default = Locale.getDefault()
        try {
            Locale.setDefault(es)
            assertEquals(listOf("80.4", "80.0", "79.6"), texts(yAxisLabels(low = 79.6f, high = 80.4f, formatter = null)))
        } finally {
            Locale.setDefault(default)
        }
    }

    @Test
    fun `a flat line is labelled once`() {
        assertEquals(listOf("80"), texts(yAxisLabels(low = 80f, high = 80f, formatter = null)))
    }

    @Test
    fun `a caller's own format is kept, but never repeated`() {
        val labels = yAxisLabels(low = 1000.2f, high = 1000.8f, formatter = { "${it.toInt()} kg" })

        assertEquals(listOf("1000 kg"), texts(labels))
    }

    @Test
    fun `dates within one month don't all read as that month`() {
        val format = dateLabelFormatter(spanMillis = days(20), locale = es)
        val stamps = listOf(date(2026, Calendar.OCTOBER, 1), date(2026, Calendar.OCTOBER, 10), date(2026, Calendar.OCTOBER, 20))

        assertEquals(3, xAxisLabels(stamps, format).size)
        assertEquals("1 oct", format(stamps.first()).removeSuffix("."))
    }

    @Test
    fun `a long span is labelled by month, and repeated months are skipped`() {
        val format = dateLabelFormatter(spanMillis = days(400), locale = Locale.US)
        val stamps = listOf(date(2025, Calendar.JANUARY, 2), date(2025, Calendar.JANUARY, 20), date(2026, Calendar.FEBRUARY, 3))

        assertEquals(listOf("Jan 25", "Feb 26"), texts(xAxisLabels(stamps, format)))
    }

    @Test
    fun `two points don't draw the last label twice`() {
        val stamps = listOf(date(2026, Calendar.OCTOBER, 1), date(2026, Calendar.OCTOBER, 9))

        assertEquals(2, xAxisLabels(stamps, dateLabelFormatter(days(8), Locale.US)).size)
    }

    private fun days(n: Int) = n * 24L * 60 * 60 * 1000

    private fun date(year: Int, month: Int, day: Int): Long = Calendar.getInstance(TimeZone.getDefault()).apply {
        clear()
        set(year, month, day, 12, 0)
    }.timeInMillis
}
