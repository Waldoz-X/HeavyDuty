package com.example.heavyduty.feature.history.presentation.detail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.ui.LocalWeightUnit
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.core.ui.theme.PrimaryCyan
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// The card is the brand's, the same whatever theme the app is in: it's what others see.
private val CardTop = Color(0xFF0B2A33)
private val CardBottom = Color(0xFF0A0A0A)
private val CardText = Color(0xFFF2F2F2)
private val CardMuted = Color(0xFFA9B4B8)

/** A preview of the card, and the button that shares it as an image. */
@Composable
internal fun ShareWorkoutDialog(uiState: WorkoutDetailUiState, title: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var sharing by remember { mutableStateOf(false) }
    val chooserTitle = stringResource(R.string.history_share_chooser)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.history_share_title)) },
        text = {
            // Recorded into [layer] as it's drawn, so what's shared is exactly what's shown.
            Box(
                modifier = Modifier.drawWithContent {
                    layer.record { this@drawWithContent.drawContent() }
                    drawLayer(layer)
                }
            ) {
                WorkoutShareCard(uiState = uiState, title = title)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sharing,
                onClick = {
                    sharing = true
                    scope.launch {
                        val bitmap = layer.toImageBitmap().asAndroidBitmap()
                        shareImage(context, bitmap, chooserTitle)
                        sharing = false
                        onDismiss()
                    }
                }
            ) { Text(stringResource(R.string.history_share_button)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun WorkoutShareCard(uiState: WorkoutDetailUiState, title: String) {
    val session = uiState.session ?: return
    val locale = LocalConfiguration.current.locales[0]
    val lines = shareExerciseLines(uiState.exercises)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PrimaryCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = CardBottom, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("HeavyDuty", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CardText)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = CardText, maxLines = 2)
        Text(
            text = SimpleDateFormat("EEEE d MMM yyyy", locale).format(Date(session.startTime)).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.bodySmall,
            color = CardMuted
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ShareStat(
                stringResource(R.string.duration_minutes, session.totalDurationSecs / SECONDS_PER_MINUTE),
                stringResource(R.string.metric_duration)
            )
            ShareStat(uiState.workingSets.toString(), stringResource(R.string.history_detail_sets))
            ShareStat(uiState.volumeKg.formatTotalWeight(), stringResource(R.string.metric_volume))
        }
        if (lines.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = CardMuted.copy(alpha = 0.3f))
            lines.take(SHARE_MAX_EXERCISES).forEach { line -> ShareExerciseRow(line) }
            if (lines.size > SHARE_MAX_EXERCISES) {
                val more = lines.size - SHARE_MAX_EXERCISES
                Text(
                    text = pluralStringResource(R.plurals.history_share_more, more, more),
                    style = MaterialTheme.typography.bodySmall,
                    color = CardMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ShareStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryCyan)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CardMuted)
    }
}

/** The exercise's name on its own line (side by side, long names were cut), its numbers under it. */
@Composable
private fun ShareExerciseRow(line: ShareExerciseLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = line.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = CardText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = listOfNotNull(
                pluralStringResource(R.plurals.history_share_sets, line.workingSets, line.workingSets),
                line.bestSet?.let {
                    bestSetText(it)
                }
            )
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = CardMuted
        )
    }
}

/** "80 kg × 8", in the unit the set was logged in. */
@Composable
private fun bestSetText(set: WorkoutSet): String? {
    val unit = set.weightUnit ?: LocalWeightUnit.current
    val weight = set.weight?.takeIf { it > 0f }?.let { "${it.kgToRounded(unit).formatDecimal()} ${unit.label}" }
    return when {
        weight != null && set.reps != null -> "$weight × ${set.reps}"
        weight != null -> weight
        set.reps != null -> "× ${set.reps}"
        else -> null
    }
}

/** Saves [bitmap] in the cache and opens the system's share sheet with it. */
private suspend fun shareImage(context: Context, bitmap: Bitmap, chooserTitle: String) {
    val file = withContext(Dispatchers.IO) {
        File(context.cacheDir, SHARE_DIR).apply { mkdirs() }.resolve(SHARE_FILE).also { file ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, it) }
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // Also as ClipData: it's what the share sheet reads to show the image's thumbnail.
        .apply { clipData = ClipData.newRawUri(null, uri) }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}

/** Must match res/xml/file_paths.xml in the app module. */
private const val SHARE_DIR = "shared"
private const val SHARE_FILE = "workout.png"
private const val PNG_QUALITY = 100
private const val SECONDS_PER_MINUTE = 60
