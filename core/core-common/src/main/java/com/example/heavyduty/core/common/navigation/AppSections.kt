package com.example.heavyduty.core.common.navigation

/**
 * Lets code outside the app module (notifications, services) ask MainActivity to open a
 * section: put [EXTRA_OPEN_SECTION] with one of the SECTION_ values on the launch intent.
 */
object AppSections {
    const val EXTRA_OPEN_SECTION = "com.example.heavyduty.OPEN_SECTION"
    const val SECTION_WORKOUT = "workout"
    const val SECTION_TIMER = "timer"
}
