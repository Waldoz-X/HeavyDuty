package com.example.heavyduty.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * HeavyDuty Color Palette — Dark-first, premium fitness aesthetic.
 *
 * Primary: Electric cyan — energetic, modern
 * Secondary: Vivid magenta/pink — contrast and accent
 * Tertiary: Lime green — success, completion
 * Surface: Deep charcoal to near-black — premium dark feel
 */

// Primary — Electric Cyan
val PrimaryCyan = Color(0xFF00E5FF)
val PrimaryCyanDark = Color(0xFF00B8D4)
val PrimaryCyanLight = Color(0xFF6EFFFF)
val OnPrimary = Color(0xFF003544)
val PrimaryContainer = Color(0xFF004D63)
val OnPrimaryContainer = Color(0xFFB8EEFF)

// Secondary — Vivid Magenta
val SecondaryMagenta = Color(0xFFFF2D78)
val SecondaryMagentaDark = Color(0xFFD4004A)
val SecondaryMagentaLight = Color(0xFFFF6FA5)
val OnSecondary = Color(0xFF5C0028)
val SecondaryContainer = Color(0xFF7F003B)
val OnSecondaryContainer = Color(0xFFFFD9E4)

// Tertiary — Lime Green
val TertiaryLime = Color(0xFF76FF03)
val TertiaryLimeDark = Color(0xFF64DD17)
val OnTertiary = Color(0xFF1B3700)
val TertiaryContainer = Color(0xFF295000)
val OnTertiaryContainer = Color(0xFFC0FF7A)

// Error
val ErrorRed = Color(0xFFFF5252)
val OnError = Color(0xFF690005)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD6)

// Dark Surface Palette
val SurfaceDark = Color(0xFF0D0D0D)
val SurfaceDim = Color(0xFF141414)
val SurfaceContainerLowest = Color(0xFF0A0A0A)
val SurfaceContainerLow = Color(0xFF1A1A1A)
val SurfaceContainer = Color(0xFF1E1E1E)
val SurfaceContainerHigh = Color(0xFF262626)
val SurfaceContainerHighest = Color(0xFF303030)
val SurfaceBright = Color(0xFF383838)
val OnSurface = Color(0xFFE6E6E6)
val OnSurfaceVariant = Color(0xFFB0B0B0)

// Outline
val OutlineDark = Color(0xFF444444)
val OutlineVariantDark = Color(0xFF2C2C2C)

// Inverse
val InverseSurface = Color(0xFFE6E6E6)
val InverseOnSurface = Color(0xFF1A1A1A)
val InversePrimary = Color(0xFF006B80)

// Background
val BackgroundDark = Color(0xFF0A0A0A)
val OnBackgroundDark = Color(0xFFE6E6E6)

// Scrim
val Scrim = Color(0xFF000000)

// Fatigue indicator — muscle recovery status on HdBodyDiagram (not part of the M3 ColorScheme;
// "fatigued" uses the standard error red, "ready" uses the diagram's own base color).
val FatigueRecoveringAmber = Color(0xFFFFB300)

// Gradient colors for special UI elements
val GradientCyanStart = Color(0xFF00E5FF)
val GradientCyanEnd = Color(0xFF0091EA)
val GradientMagentaStart = Color(0xFFFF2D78)
val GradientMagentaEnd = Color(0xFFAA00FF)
val GradientLimeStart = Color(0xFF76FF03)
val GradientLimeEnd = Color(0xFF00E676)

// ---------------------------------------------------------------- light theme
// The same three accents, deepened until they read as text and icons on white (the electric
// cyan, magenta and lime of the dark theme wash out there), over light-gray surfaces where
// cards are white and anything raised inside a card is a step darker.
val LightPrimary = Color(0xFF006A7A)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFA6EEFF)
val LightOnPrimaryContainer = Color(0xFF001F26)
val LightSecondary = Color(0xFFB0004E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFFFD9E2)
val LightOnSecondaryContainer = Color(0xFF3E0019)
val LightTertiary = Color(0xFF3B6A00)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFB8F57C)
val LightOnTertiaryContainer = Color(0xFF0E2000)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)
val LightBackground = Color(0xFFF2F4F5)
val LightOnBackground = Color(0xFF171C1E)
val LightSurface = Color(0xFFF2F4F5)
val LightSurfaceDim = Color(0xFFD8DCDE)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFAFBFB)
val LightSurfaceContainer = Color(0xFFF6F8F9)
val LightSurfaceContainerHigh = Color(0xFFFFFFFF)
val LightSurfaceContainerHighest = Color(0xFFE6EAEC)
val LightSurfaceBright = Color(0xFFFFFFFF)

// Neutral, like the rest of the surfaces; left out, Material falls back to its lavender.
val LightSurfaceVariant = Color(0xFFDDE3E5)
val LightOnSurface = Color(0xFF171C1E)
val LightOnSurfaceVariant = Color(0xFF40484B)
val LightOutline = Color(0xFF70787C)
val LightOutlineVariant = Color(0xFFC0C8CB)
val LightInverseSurface = Color(0xFF2C3133)
val LightInverseOnSurface = Color(0xFFEDF1F3)
val LightInversePrimary = Color(0xFF00E5FF)
