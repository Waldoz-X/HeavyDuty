package com.example.heavyduty.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the release build with and without the Baseline Profile:
 *   ./gradlew :baselineprofile:connectedBenchmarkReleaseAndroidTest
 * Compare `timeToInitialDisplayMs` (startup) and `frameDurationCpuMs` / `frameOverrunMs` (scroll)
 * between the *NoCompilation and *BaselineProfile results. Use a physical device for real numbers.
 */
@RunWith(AndroidJUnit4::class)
class PerformanceBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startupNoCompilation() = startup(CompilationMode.None())

    @Test
    fun startupBaselineProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    @Test
    fun scrollExercisesNoCompilation() = scrollExercises(CompilationMode.None())

    @Test
    fun scrollExercisesBaselineProfile() = scrollExercises(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
        waitForHome()
    }

    private fun scrollExercises(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = compilationMode,
        // No startupMode: COLD would kill the process right before measuring, after setupBlock
        // opened the list, leaving nothing on screen to scroll.
        startupMode = null,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            waitForHome()
            openExercises()
        },
    ) {
        flingMainList()
    }
}
