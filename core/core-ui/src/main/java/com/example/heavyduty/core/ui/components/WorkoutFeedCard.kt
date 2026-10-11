package com.example.heavyduty.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.heavyduty.core.common.R
import com.example.heavyduty.core.ui.formatTotalWeight

private const val MAX_VISIBLE_EXERCISES = 4

/**
 * Social-feed style card summarizing a completed workout.
 */
@Composable
fun WorkoutFeedCard(item: WorkoutFeedItem, modifier: Modifier = Modifier) {
    HdCard(modifier = modifier.fillMaxWidth()) {
        // Header: avatar, name, relative time
        Row(verticalAlignment = Alignment.CenterVertically) {
            FeedAvatar(name = item.userName, avatarUrl = item.userAvatarUrl)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.userName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.relativeTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = item.workoutName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Key metrics
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FeedMetric(icon = Icons.Default.Timer, text = stringResource(R.string.duration_minutes, item.durationMinutes))
            FeedMetric(icon = Icons.Default.MonitorWeight, text = item.volumeKg.formatTotalWeight())
            if (item.personalRecords > 0) {
                FeedMetric(
                    icon = Icons.Default.EmojiEvents,
                    text = pluralStringResource(R.plurals.feed_personal_records, item.personalRecords, item.personalRecords),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (item.exercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item.exercises.take(MAX_VISIBLE_EXERCISES).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val remaining = item.exercises.size - MAX_VISIBLE_EXERCISES
                if (remaining > 0) {
                    Text(
                        text = stringResource(R.string.feed_more_exercises, remaining),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedAvatar(name: String, avatarUrl: String?) {
    if (avatarUrl != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
        )
    } else {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun FeedMetric(
    icon: ImageVector,
    text: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}
