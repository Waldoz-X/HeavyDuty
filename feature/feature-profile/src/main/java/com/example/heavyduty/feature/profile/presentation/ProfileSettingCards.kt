package com.example.heavyduty.feature.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.domain.model.WeightUnit
import com.example.heavyduty.core.ui.components.HdCard
import com.example.heavyduty.core.ui.components.HdChip
import com.example.heavyduty.core.ui.components.HdTonalButton

/** A Perfil setting: its name, a line on what it does, and its controls below. */
@Composable
internal fun SettingCard(title: String, hint: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(text = hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

/** Mutually exclusive options as chips; [options] pairs each value with its label. */
@Composable
internal fun <T> ChoiceChips(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) -> HdChip(text = label, isSelected = value == selected, onClick = { onSelect(value) }) }
    }
}

@Composable
internal fun BodyFigureSetting(gender: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    SettingCard(stringResource(R.string.profile_body_figure), stringResource(R.string.profile_body_figure_hint), modifier) {
        ChoiceChips(
            options = listOf(
                "male" to stringResource(R.string.profile_body_male),
                "female" to stringResource(R.string.profile_body_female)
            ),
            selected = gender,
            onSelect = onChange
        )
    }
}

/** On/off for the effort input, and — when on — whether it's logged as RIR or RPE. */
@Composable
internal fun EffortSetting(
    enabled: Boolean,
    scale: String,
    onEnabledChange: (Boolean) -> Unit,
    onScaleChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingCard(stringResource(R.string.profile_effort), stringResource(R.string.profile_effort_hint), modifier) {
        ChoiceChips(
            options = listOf(false to stringResource(R.string.profile_off), true to stringResource(R.string.profile_on)),
            selected = enabled,
            onSelect = onEnabledChange
        )
        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))
            ChoiceChips(options = listOf("RIR" to "RIR", "RPE" to "RPE"), selected = scale, onSelect = onScaleChange)
        }
    }
}

@Composable
internal fun WeightUnitSetting(unit: WeightUnit, onChange: (WeightUnit) -> Unit, modifier: Modifier = Modifier) {
    SettingCard(stringResource(R.string.profile_weight_unit), stringResource(R.string.profile_weight_unit_hint), modifier) {
        ChoiceChips(
            options = listOf(
                WeightUnit.KG to stringResource(R.string.profile_unit_kg),
                WeightUnit.LB to stringResource(R.string.profile_unit_lb)
            ),
            selected = unit,
            onSelect = onChange
        )
    }
}

/** Export and import of the backup file, with what the last one did (tap the message to hide it). */
@Composable
internal fun YourDataCard(
    isBusy: Boolean,
    message: String?,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingCard(stringResource(R.string.profile_your_data), stringResource(R.string.profile_your_data_hint), modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HdTonalButton(
                text = stringResource(R.string.profile_export),
                onClick = onExportClick,
                icon = Icons.Default.FileDownload,
                enabled = !isBusy
            )
            HdTonalButton(
                text = stringResource(R.string.profile_import),
                onClick = onImportClick,
                icon = Icons.Default.FileUpload,
                enabled = !isBusy
            )
        }
        if (isBusy) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.profile_processing),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onDismissMessage)
            )
        }
    }
}
