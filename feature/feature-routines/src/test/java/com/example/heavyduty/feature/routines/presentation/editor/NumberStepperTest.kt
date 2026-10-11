package com.example.heavyduty.feature.routines.presentation.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The − value + control of the routine editor, as the lifter uses it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "es")
class NumberStepperTest {

    @get:Rule
    val compose = createComposeRule()

    private var value by mutableFloatStateOf(0f)

    private fun show(initial: Float, step: Float = 2.5f, range: ClosedFloatingPointRange<Float> = 0f..500f, decimals: Boolean = true) {
        value = initial
        compose.setContent {
            HeavyDutyTheme {
                NumberStepper(label = "Peso", value = value, step = step, range = range, onValueChange = {
                    value = it
                }, decimals = decimals)
            }
        }
    }

    @Test
    fun `plus and minus move by the step and land on round numbers`() {
        show(initial = 61f)

        compose.onNodeWithContentDescription("Aumentar").performClick()
        assertEquals(62.5f, value)
        compose.onNodeWithContentDescription("Disminuir").performClick()
        compose.onNodeWithContentDescription("Disminuir").performClick()
        assertEquals(57.5f, value)
        compose.onNodeWithText("57.5").assertExists()
    }

    @Test
    fun `minus stops at the bottom of the range`() {
        show(initial = 0f)

        compose.onNodeWithContentDescription("Disminuir").assertIsNotEnabled()
    }

    @Test
    fun `typing after tapping the number replaces it`() {
        show(initial = 0f, step = 1f, decimals = false)

        compose.onNode(hasSetTextAction()).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("180")

        assertEquals(180f, value)
    }

    @Test
    fun `a decimal comma is read as a point, other characters are ignored`() {
        show(initial = 0f)

        compose.onNode(hasSetTextAction()).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("62,5kg")

        assertEquals(62.5f, value)
    }

    @Test
    fun `a typed number beyond the range is held at its edge`() {
        show(initial = 10f, step = 1f, range = 1f..20f, decimals = false)

        compose.onNode(hasSetTextAction()).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("99")

        assertEquals(20f, value)
    }
}
