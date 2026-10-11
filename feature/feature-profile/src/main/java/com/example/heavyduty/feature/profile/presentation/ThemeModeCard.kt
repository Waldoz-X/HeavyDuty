package com.example.heavyduty.feature.profile.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.ThemeMode

/** Dark (the default), light, or whatever the phone uses. */
@Composable
internal fun ThemeModeCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    SettingCard(stringResource(R.string.profile_theme), stringResource(R.string.profile_theme_hint), modifier) {
        ChoiceChips(
            options = listOf(
                ThemeMode.DARK to stringResource(R.string.profile_theme_dark),
                ThemeMode.LIGHT to stringResource(R.string.profile_theme_light),
                ThemeMode.SYSTEM to stringResource(R.string.profile_theme_system)
            ),
            selected = selected,
            onSelect = onSelect
        )
    }
}
