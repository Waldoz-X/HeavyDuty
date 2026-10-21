package com.example.heavyduty.feature.history.presentation.detail

import android.Manifest
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.common.extensions.formatDecimal
import com.example.heavyduty.core.domain.model.WorkoutSet
import com.example.heavyduty.core.domain.model.kgToRounded
import com.example.heavyduty.core.domain.model.label
import com.example.heavyduty.core.ui.LocalWeightUnit
import com.example.heavyduty.core.ui.components.BodyGender
import com.example.heavyduty.core.ui.components.HdBodyDiagram
import com.example.heavyduty.core.ui.components.HdButton
import com.example.heavyduty.core.ui.components.HdTonalButton
import com.example.heavyduty.core.ui.formatTotalWeight
import com.example.heavyduty.core.ui.share.ShareImageCanvas
import com.example.heavyduty.core.ui.share.ShareImageFormat
import com.example.heavyduty.core.ui.share.saveImageToGallery
import com.example.heavyduty.core.ui.share.savingToGalleryNeedsPermission
import com.example.heavyduty.core.ui.share.shareImage
import com.example.heavyduty.core.ui.theme.HeavyDutyTheme
import com.example.heavyduty.core.ui.theme.PrimaryCyan
import com.example.heavyduty.feature.history.domain.model.ShareExerciseLine
import com.example.heavyduty.feature.history.domain.model.ShareRecord
import com.example.heavyduty.feature.history.domain.model.WorkoutShareSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

// The card is the brand's, the same whatever theme the app is in: it's what others see.
private val CardTop = Color(0xFF0B2A33)
private val CardBottom = Color(0xFF0A0A0A)
private val CardText = Color(0xFFF2F2F2)
private val CardMuted = Color(0xFFA9B4B8)
private val CardBody = Color(0xFF1F2B2F)
private val RecordGold = Color(0xFFFFC94D)

/**
 * How many rows each format lists. The card is laid out at a fixed size, density and font scale
 * (see [ShareImageCanvas]), so these hold on every phone: with a 2-line title and every row
 * filled, the text still fits, and the body map takes whatever height is left.
 */
private data class CardRoom(val records: Int, val exercises: Int)

private val ShareImageFormat.room: CardRoom
    get() = when (this) {
        ShareImageFormat.STORY -> CardRoom(records = 3, exercises = 6)
        ShareImageFormat.POST -> CardRoom(records = 1, exercises = 4)
    }

/** Below this the silhouettes are too small to read, so the space is left empty instead. */
private val MinBodyMapHeight = 110.dp

/** Full screen: the card as it will be exported, its format, and saving or sharing it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareWorkoutDialog(uiState: WorkoutDetailUiState, title: String, onDismiss: () -> Unit) {
    var format by rememberSaveable { mutableStateOf(ShareImageFormat.STORY) }
    val export = rememberCardExport(fileName = shareFileName(uiState.session?.startTime ?: 0L, format))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.history_share_title)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_close))
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(export.snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FormatSelector(selected = format, onSelect = { format = it })
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val summary = uiState.shareSummary
                    if (summary == null) {
                        Text(stringResource(R.string.history_share_preparing), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        ShareImageCanvas(format = format, layer = export.layer, modifier = Modifier.clip(RoundedCornerShape(12.dp))) {
                            WorkoutShareCard(summary = summary, title = title, format = format, gender = uiState.bodyGender)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    val ready = uiState.shareSummary != null && !export.busy
                    HdTonalButton(
                        text = stringResource(R.string.history_share_save),
                        onClick = export.save,
                        enabled = ready,
                        icon = Icons.Default.Download,
                        modifier = Modifier.weight(1f)
                    )
                    HdButton(
                        text = stringResource(R.string.history_share),
                        onClick = export.share,
                        enabled = ready,
                        icon = Icons.Default.Share,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatSelector(selected: ShareImageFormat, onSelect: (ShareImageFormat) -> Unit) {
    val formats = ShareImageFormat.entries
    SingleChoiceSegmentedButtonRow {
        formats.forEachIndexed { index, format ->
            SegmentedButton(
                selected = format == selected,
                onClick = { onSelect(format) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size)
            ) {
                Text(
                    stringResource(
                        when (format) {
                            ShareImageFormat.STORY -> R.string.history_share_format_story
                            ShareImageFormat.POST -> R.string.history_share_format_post
                        }
                    )
                )
            }
        }
    }
}

/** The recorded card, and saving or sharing it as a full-resolution PNG. */
private class CardExport(
    val layer: GraphicsLayer,
    val snackbarHostState: SnackbarHostState,
    val busy: Boolean,
    val save: () -> Unit,
    val share: () -> Unit
)

@Composable
private fun rememberCardExport(fileName: String): CardExport {
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    val snackbarHostState = remember { SnackbarHostState() }
    var busy by remember { mutableStateOf(false) }

    fun run(action: suspend (Bitmap) -> Unit) {
        busy = true
        scope.launch {
            try {
                action(layer.toImageBitmap().asAndroidBitmap())
            } finally {
                busy = false
            }
        }
    }

    val saveNow = {
        run { bitmap ->
            val saved = saveImageToGallery(context, bitmap, fileName)
            snackbarHostState.showSnackbar(
                resources.getString(if (saved) R.string.history_share_saved else R.string.history_share_save_failed)
            )
        }
    }
    // Only asked for before Android 10; later, an app's own images need no permission.
    val askStorage = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            saveNow()
        } else {
            scope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.history_share_save_failed)) }
        }
    }
    val chooserTitle = stringResource(R.string.history_share_chooser)
    return CardExport(
        layer = layer,
        snackbarHostState = snackbarHostState,
        busy = busy,
        save = { if (savingToGalleryNeedsPermission) askStorage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else saveNow() },
        share = { run { bitmap -> shareImage(context, bitmap, chooserTitle) } }
    )
}

/** "HeavyDuty-20261020-1830-story": sorts by date in the gallery, and says which format it is. */
private fun shareFileName(startTime: Long, format: ShareImageFormat): String =
    "HeavyDuty-${SimpleDateFormat("yyyyMMdd-HHmm", Locale.ROOT).format(Date(startTime))}-${format.name.lowercase(Locale.ROOT)}"

/** The card itself, designed 360 dp wide; the format only sets its height and what fits. */
@Composable
private fun WorkoutShareCard(summary: WorkoutShareSummary, title: String, format: ShareImageFormat, gender: BodyGender) {
    val room = format.room
    // Dark whatever the app's theme: the body map's labels read from the theme.
    HeavyDutyTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
                .padding(24.dp)
        ) {
            BrandRow()
            Spacer(modifier = Modifier.height(16.dp))
            CardHeader(title = title, startTime = summary.startTime)
            Spacer(modifier = Modifier.height(16.dp))
            StatsRow(summary)
            if (summary.records.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                RecordsBlock(records = summary.records, shown = room.records)
            }
            // Whatever height the text leaves, so the card is full however much the workout has.
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 16.dp)) {
                if (summary.setsByMuscle.isNotEmpty() && maxHeight >= MinBodyMapHeight) {
                    HdBodyDiagram(
                        muscleValues = summary.setsByMuscle,
                        gender = gender,
                        baseColor = CardBody,
                        highlightColor = PrimaryCyan,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            if (summary.exercises.isNotEmpty()) ExerciseList(lines = summary.exercises, shown = room.exercises)
        }
    }
}

@Composable
private fun BrandRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(PrimaryCyan),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = CardBottom, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text("HeavyDuty", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = CardText)
    }
}

@Composable
private fun CardHeader(title: String, startTime: Long) {
    val locale = LocalConfiguration.current.locales[0]
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CardText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = SimpleDateFormat("EEEE d MMM yyyy", locale).format(Date(startTime)).replaceFirstChar { it.titlecase(locale) },
            style = MaterialTheme.typography.bodySmall,
            color = CardMuted
        )
    }
}

@Composable
private fun StatsRow(summary: WorkoutShareSummary) {
    Row(modifier = Modifier.fillMaxWidth()) {
        ShareStat(
            value = stringResource(R.string.duration_minutes, summary.durationSecs / SECONDS_PER_MINUTE),
            label = stringResource(R.string.metric_duration),
            modifier = Modifier.weight(1f)
        )
        ShareStat(summary.workingSets.toString(), stringResource(R.string.history_detail_sets), Modifier.weight(1f))
        ShareStat(summary.volumeKg.formatTotalWeight(), stringResource(R.string.metric_volume), Modifier.weight(1f))
    }
}

@Composable
private fun ShareStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PrimaryCyan, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = CardMuted, maxLines = 1)
    }
}

/** "🏆 2 new records", then the first [shown] of them with the set that set each. */
@Composable
private fun RecordsBlock(records: List<ShareRecord>, shown: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RecordGold, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = pluralStringResource(R.plurals.history_share_records, records.size, records.size),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = RecordGold
            )
        }
        records.take(shown).forEach { record ->
            CardRow(name = record.exerciseName, detail = bestSetText(record.set).orEmpty(), detailColor = RecordGold)
        }
    }
}

@Composable
private fun ExerciseList(lines: List<ShareExerciseLine>, shown: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        lines.take(shown).forEach { line ->
            val detail = listOfNotNull(
                pluralStringResource(R.plurals.history_share_sets, line.workingSets, line.workingSets),
                line.bestSet?.let { bestSetText(it) }
            ).joinToString(" · ")
            CardRow(name = line.name, detail = detail, detailColor = CardMuted)
        }
        if (lines.size > shown) {
            val more = lines.size - shown
            Text(
                text = pluralStringResource(R.plurals.history_share_more, more, more),
                style = MaterialTheme.typography.bodySmall,
                color = CardMuted
            )
        }
    }
}

/** One line, always: a long exercise name is shortened, its numbers never are. */
@Composable
private fun CardRow(name: String, detail: String, detailColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = CardText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = detail, style = MaterialTheme.typography.bodySmall, color = detailColor, maxLines = 1)
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

private const val SECONDS_PER_MINUTE = 60
