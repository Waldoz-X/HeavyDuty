package com.example.heavyduty.feature.timer.presentation

import androidx.annotation.StringRes
import com.example.heavyduty.core.common.R
import com.example.heavyduty.feature.timer.domain.model.TimerMode

val TimerMode.nameResId: Int
    @StringRes get() = when (this) {
        TimerMode.REST -> R.string.timer_mode_rest
        TimerMode.STOPWATCH -> R.string.timer_mode_stopwatch
        TimerMode.AMRAP -> R.string.timer_mode_amrap
        TimerMode.EMOM -> R.string.timer_mode_emom
        TimerMode.TABATA -> R.string.timer_mode_tabata
    }

val TimerMode.descriptionResId: Int
    @StringRes get() = when (this) {
        TimerMode.REST -> R.string.timer_desc_rest
        TimerMode.STOPWATCH -> R.string.timer_desc_stopwatch
        TimerMode.AMRAP -> R.string.timer_desc_amrap
        TimerMode.EMOM -> R.string.timer_desc_emom
        TimerMode.TABATA -> R.string.timer_desc_tabata
    }
