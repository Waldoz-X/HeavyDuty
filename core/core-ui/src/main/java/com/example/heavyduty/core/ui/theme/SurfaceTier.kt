package com.example.heavyduty.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Names *intent* on top of Material 3's tonal surfaceContainer roles, so a call site picks a
 * level by what it's building rather than reaching for a raw surfaceContainer* role from habit
 * (which is how the whole app ended up on surfaceContainerHigh everywhere, flattening the
 * hierarchy between a card and whatever sits nested inside it).
 *
 * - [base]: the screen/Scaffold background.
 * - [card]: a first-level card floating on the background — what HdCard uses by default.
 * - [raised]: content nested *inside* a card (a selected row, a highlighted item) — one tonal
 *   step up so it doesn't blend into its parent card.
 * - [overlay]: a dialog or bottom sheet floating above the screen.
 */
val ColorScheme.base: Color get() = background
val ColorScheme.card: Color get() = surfaceContainerHigh
val ColorScheme.raised: Color get() = surfaceContainerHighest
val ColorScheme.overlay: Color get() = surfaceContainerLow
