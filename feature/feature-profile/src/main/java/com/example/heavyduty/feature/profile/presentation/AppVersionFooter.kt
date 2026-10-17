package com.example.heavyduty.feature.profile.presentation

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.heavyduty.core.common.R

/** "HeavyDuty 0.1.0 (175)" — what someone reporting a problem should quote. */
@Composable
internal fun AppVersionFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { context.appVersion() } ?: return
    Text(
        text = stringResource(R.string.profile_app_version, version.first, version.second),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

/** Name and code from the installed package, read at runtime: no generated BuildConfig needed. */
private fun Context.appVersion(): Pair<String, Long>? = runCatching {
    val info = packageManager.getPackageInfo(packageName, 0)
    val code = if (Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.P
    ) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    info.versionName.orEmpty() to code
}.getOrNull()
