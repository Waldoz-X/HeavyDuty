package com.example.heavyduty.core.common.extensions

import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class NumberExtensionsTest {

    private lateinit var originalLocale: Locale

    @Before
    fun useSpanishLocale() {
        // The app runs in Spanish, where the default decimal separator is a comma.
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("es"))
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `decimals use a point whatever the phone's language`() {
        assertEquals("62.5", 62.5f.formatDecimal())
        assertEquals("75.0", 75f.formatOneDecimal())
        assertEquals("12.5k", 12_500f.formatCompact())
    }

    @Test
    fun `whole numbers have no decimal part`() {
        assertEquals("60", 60f.formatDecimal())
        assertEquals("950", 950f.formatCompact())
    }

    @Test
    fun `values a hair off a whole number read as whole`() {
        // Fractional set credit adds up to things like 11.999997 (0.6 + 0.2 + 0.2 ...).
        assertEquals("12", 11.999997f.formatDecimal())
        assertEquals("12", 12.000003f.formatDecimal())
        assertEquals("5.4", 5.4000006f.formatDecimal())
        assertEquals("7k", 6_999.99f.formatCompact())
    }
}
