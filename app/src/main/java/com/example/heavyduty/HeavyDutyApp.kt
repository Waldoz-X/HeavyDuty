package com.example.heavyduty

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import com.example.heavyduty.core.domain.repository.TrainingReminderScheduler
import com.example.heavyduty.core.domain.repository.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class HeavyDutyApp : Application() {

    @Inject lateinit var userPreferences: UserPreferencesRepository

    @Inject lateinit var reminderScheduler: TrainingReminderScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        defaultToSpanish()
        // A force-stop or reinstall drops the alarm; opening the app sets it again.
        appScope.launch { reminderScheduler.schedule(userPreferences.trainingReminder.first()) }
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
