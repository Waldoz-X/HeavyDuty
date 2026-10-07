package com.example.heavyduty.core.domain.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ExerciseDisplayNameTest {

    private val originalLocale = Locale.getDefault()
    private val bench = Exercise(name = "Press de banca con barra", nameEn = "barbell bench press", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)
    private val custom = Exercise(name = "Mi press raro", muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH, isCustom = true)

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    @Test
    fun `spanish shows the stored name`() {
        Locale.setDefault(Locale.forLanguageTag("es"))
        assertEquals("Press de banca con barra", bench.displayName)
    }

    @Test
    fun `english shows the catalog's english name, capitalized`() {
        Locale.setDefault(Locale.forLanguageTag("en"))
        assertEquals("Barbell bench press", bench.displayName)
    }

    @Test
    fun `custom exercises always show what the user typed`() {
        Locale.setDefault(Locale.forLanguageTag("en"))
        assertEquals("Mi press raro", custom.displayName)
    }
}
