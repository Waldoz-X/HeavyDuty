package com.example.heavyduty.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

/** User journeys shared by the Baseline Profile generator and the benchmarks. */

const val PACKAGE_NAME = "com.waldoz.heavyduty"

private const val TIMEOUT_MS = 10_000L
private const val SEED_TIMEOUT_MS = 60_000L

/** Waits for the bottom bar, i.e. the first screen is drawn and interactive. */
fun MacrobenchmarkScope.waitForHome() {
    device.wait(Until.hasObject(By.text("Inicio")), TIMEOUT_MS)
}

/** Opens a bottom-bar tab by its label ("Inicio", "Entrenar", "Ejercicios", "Temporizador"). */
fun MacrobenchmarkScope.openTab(label: String) {
    device.wait(Until.findObject(By.text(label)), TIMEOUT_MS)?.click()
    device.waitForIdle()
}

/** Flings the screen's main list down a few times and back up. */
fun MacrobenchmarkScope.flingMainList(times: Int = 3) {
    repeat(times) { fling(Direction.DOWN) }
    fling(Direction.UP)
}

/**
 * Looks the list up again for every gesture: while a screen is still loading, Compose replaces
 * its nodes and a list found a moment earlier is gone (StaleObjectException).
 */
private fun MacrobenchmarkScope.fling(direction: Direction) {
    repeat(3) {
        val list = mainScrollable() ?: return
        try {
            // Keep gestures away from the edges so they don't trigger system back or the drawer.
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(direction)
            device.waitForIdle()
            return
        } catch (_: StaleObjectException) {
            device.waitForIdle()
        }
    }
}

fun MacrobenchmarkScope.browseDashboard() {
    openTab("Inicio")
    flingMainList(times = 2)
}

fun MacrobenchmarkScope.browseExercises() {
    openExercises()
    flingMainList()
}

/**
 * Opens the exercise list and waits until it's filled. Right after install the catalog is
 * still being seeded and the list is empty ("0 ejercicios"), with nothing to scroll.
 */
fun MacrobenchmarkScope.openExercises() {
    openTab("Ejercicios")
    device.wait(Until.hasObject(By.text(Pattern.compile("[1-9]\\d* ejercicios"))), SEED_TIMEOUT_MS)
    device.waitForIdle()
}

fun MacrobenchmarkScope.browseWorkoutTab() {
    openTab("Entrenar")
    flingMainList(times = 1)
}

/** The tallest scrollable node: the screen's list, not a horizontal row of filter chips. */
private fun MacrobenchmarkScope.mainScrollable(): UiObject2? {
    device.wait(Until.hasObject(By.scrollable(true)), TIMEOUT_MS)
    return device.findObjects(By.scrollable(true)).maxByOrNull { it.visibleBounds.height() }
}
