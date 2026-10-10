package com.example.heavyduty.core.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.heavyduty.core.domain.model.Exercise
import com.example.heavyduty.core.domain.model.displayName

/**
 * Square exercise thumbnail: the still image when there is one, else the first frame of the
 * demo GIF, else a placeholder icon. The catalog images are drawn on white, so the tile keeps
 * a white background to avoid a dark frame around the figure.
 */
@Composable
fun HdExerciseThumbnail(exercise: Exercise?, size: Dp, modifier: Modifier = Modifier) {
    val url = exercise?.imageUrl?.ifBlank { exercise.gifUrl }.orEmpty()
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(if (url.isNotBlank()) Color.White else MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = exercise?.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(size / 5)
            )
        }
    }
}

/** The exercise's animated demo (GIF), falling back to the still image. */
@Composable
fun HdExerciseAnimation(exercise: Exercise, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val url = exercise.gifUrl.ifBlank { exercise.imageUrl }
    val request = remember(url) {
        ImageRequest.Builder(context)
            .data(url)
            .decoderFactory(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AnimatedImageDecoder.Factory() else GifDecoder.Factory()
            )
            .crossfade(true)
            .build()
    }
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNotBlank()) {
            AsyncImage(
                model = request,
                contentDescription = exercise.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
