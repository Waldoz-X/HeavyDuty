package com.example.heavyduty.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.heavyduty.core.ui.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val plusJakartaSans = GoogleFont("Plus Jakarta Sans")

/**
 * Downloaded on-device via Google Play Services (ui-text-google-fonts) — no font binary is
 * bundled in the APK. If Play Services is unavailable the request simply never resolves and
 * text keeps rendering in the platform default typeface; it doesn't crash or block rendering.
 *
 * Plus Jakarta Sans ships up to ExtraBold (800), no true Black (900) — Type.kt's displayLarge
 * asks for [FontWeight.Black], which FontFamily resolves to the nearest weight listed here
 * (ExtraBold), same as it would for any other requested weight not explicitly listed.
 */
val HeavyDutyFontFamily = FontFamily(
    Font(googleFont = plusJakartaSans, fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = plusJakartaSans, fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = plusJakartaSans, fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = plusJakartaSans, fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = plusJakartaSans, fontProvider = googleFontProvider, weight = FontWeight.ExtraBold)
)
