package com.example.heavyduty.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.heavyduty.core.common.R

/**
 * Shown instead of a screen's content when its data couldn't be read — so a failure never
 * looks like "you have no data yet", and the user sees a message in their language rather
 * than an exception's text.
 */
@Composable
fun HdErrorState(modifier: Modifier = Modifier) {
    HdEmptyState(
        icon = Icons.Outlined.ErrorOutline,
        title = stringResource(R.string.error_load_title),
        description = stringResource(R.string.error_load_desc),
        modifier = modifier
    )
}
