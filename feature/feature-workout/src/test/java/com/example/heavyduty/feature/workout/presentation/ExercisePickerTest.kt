package com.example.heavyduty.feature.workout.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.ExerciseType
import com.example.heavyduty.core.domain.model.MuscleGroup
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35], qualifiers = "es")
class ExercisePickerTest {

    @get:Rule
    val compose = createComposeRule()

    private fun exercise(id: Long, name: String) =
        Exercise(id = id, name = name, muscleGroup = MuscleGroup.CHEST, exerciseType = ExerciseType.STRENGTH)

    @Test
    fun `a new search starts at its best match, not where the last one was scrolled to`() {
        val variants = (1L..20L).map { exercise(it, "Press de banca variante $it") }
        var results by mutableStateOf(variants)
        compose.setContent {
            HeavyDutyTheme { ExercisePicker(query = "", results = results, onQueryChange = {}, onExercisePick = {}) }
        }
        compose.onNodeWithTag(EXERCISE_PICKER_LIST).performScrollToIndex(15)

        // A narrower search: the best match comes first, the exercise that was on screen is still in the list.
        results = listOf(exercise(99, "Press de banca con barra")) + variants.drop(10)
        compose.waitForIdle()

        compose.onNodeWithText("Press de banca con barra").assertIsDisplayed()
    }
}
