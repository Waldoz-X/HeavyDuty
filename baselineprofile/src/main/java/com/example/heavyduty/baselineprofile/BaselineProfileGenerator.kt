package com.example.heavyduty.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the classes and methods used on startup and the main screens, so they're compiled
 * ahead of time instead of interpreted/JIT-compiled the first times the user opens them.
 *
 * Regenerate after big UI changes with an emulator (API 33+) or device connected:
 *   ./gradlew :app:generateBaselineProfile
 * Output: app/src/release/generated/baselineProfiles/ (commit it).
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = PACKAGE_NAME,
        // Also produce a startup profile, used to lay out the dex for faster startup.
        includeInStartupProfile = true,
    ) {
        pressHome()
        startActivityAndWait()
        waitForHome()

        browseDashboard()
        browseExercises()
        browseWorkoutTab()
    }
}
