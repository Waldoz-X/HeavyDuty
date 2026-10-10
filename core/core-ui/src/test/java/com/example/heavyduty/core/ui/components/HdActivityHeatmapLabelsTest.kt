package com.example.heavyduty.core.ui.components

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class HdActivityHeatmapLabelsTest {

    private fun label(x: Float, text: String = "Oct") = MonthLabel(x = x, width = 20f, text = text)

    @Test
    fun `a label that would run past the right edge is left out instead of wrapping`() {
        // What the map showed: the final "Oct" squeezed into the last column, one letter per line.
        val labels = listOf(label(0f, "Oct"), label(100f, "Sept"), label(290f, "Oct"))

        assertEquals(listOf("Oct", "Sept"), labelsThatFit(labels, availableWidth = 300f, gap = 4f).map { it.text })
    }

    @Test
    fun `a label that would overlap the previous one is left out`() {
        val labels = listOf(label(0f, "Oct"), label(15f, "Nov"), label(30f, "Dic"))

        assertEquals(listOf("Oct", "Dic"), labelsThatFit(labels, availableWidth = 300f, gap = 4f).map { it.text })
    }

    @Test
    fun `labels with room are all kept`() {
        val labels = List(12) { label(it * 25f) }

        assertEquals(12, labelsThatFit(labels, availableWidth = 300f, gap = 4f).size)
    }

    @Test
    fun `a column is marked where each month begins, plus the first one`() {
        val grid = weeksFrom(year = 2026, month = Calendar.SEPTEMBER, day = 14, weeks = 6) // Mondays 14 Sep .. 19 Oct

        assertEquals(listOf(0, 3), monthStartColumns(grid).map { it.first })
    }

    @Test
    fun `columns that start in the future are skipped`() {
        val grid = weeksFrom(year = 2026, month = Calendar.SEPTEMBER, day = 14, weeks = 2) + listOf(List(7) { null })

        assertEquals(listOf(0), monthStartColumns(grid).map { it.first })
    }

    private fun weeksFrom(year: Int, month: Int, day: Int, weeks: Int): List<List<Long?>> {
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month, day)
        }
        return List(weeks) {
            List(7) {
                cal.timeInMillis.also { cal.add(Calendar.DAY_OF_YEAR, 1) }
            }
        }
    }
}
