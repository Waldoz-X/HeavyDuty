package com.example.heavyduty

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HeavyDutyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        defaultToSpanish()
    }

    /**
     * The app is Spanish-first — the exercise catalog and several screens only exist in
     * Spanish — so it starts in Spanish whatever the phone's language, instead of mixing
     * English labels into Spanish screens. On Android 13+ the user can still pick English in
     * the system's "App languages" setting (see res/xml/locales_config.xml); that choice is
     * kept, since this only fills in a language when none was chosen.
     */
    private fun defaultToSpanish() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val localeManager = getSystemService(LocaleManager::class.java) ?: return
        if (localeManager.applicationLocales.isEmpty) {
            localeManager.applicationLocales = LocaleList.forLanguageTags(SPANISH)
        }
    }

    companion object {
        const val SPANISH = "es"
    }
}
