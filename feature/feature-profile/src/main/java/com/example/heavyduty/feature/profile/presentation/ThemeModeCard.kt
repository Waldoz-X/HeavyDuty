package com.example.heavyduty.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.ThemeMode
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip

/** Dark (the default), light, or whatever the phone uses. */
@Composable
internal fun ThemeModeCard(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.profile_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = stringResource(R.string.profile_theme_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                ThemeMode.DARK to R.string.profile_theme_dark,
                ThemeMode.LIGHT to R.string.profile_theme_light,
                ThemeMode.SYSTEM to R.string.profile_theme_system
            ).forEach { (mode, label) ->
                HdChip(text = stringResource(label), isSelected = selected == mode, onClick = { onSelect(mode) })
            }
        }
    }
}
