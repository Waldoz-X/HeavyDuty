package com.example.heavyduty.core.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Loading placeholders shaped like the content that's coming ("skeletons"): the screen keeps
 * its layout while data loads, so nothing jumps when it arrives.
 *
 * They shimmer with [shimmer], which repaints without recomposing and stops as soon as the
 * skeleton leaves the screen.
 */
@Composable
fun HdSkeletonBlock(modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    Box(modifier = modifier.clip(shape).shimmer())
}

/** A list of card-shaped rows: thumbnail plus two lines of text, like most of the app's lists. */
@Composable
fun HdCardListSkeleton(modifier: Modifier = Modifier, rows: Int = 6, thumbnailSize: Dp = 64.dp, contentPadding: Dp = 0.dp) {
    Column(
        // One unit for TalkBack instead of a dozen unlabeled boxes.
        modifier = modifier
            .padding(horizontal = contentPadding)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HdSkeletonBlock(modifier = Modifier.size(thumbnailSize), shape = RoundedCornerShape(14.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Varying widths read as text rather than as a grid of bars.
                    HdSkeletonBlock(modifier = Modifier.fillMaxWidth(if (index % 2 == 0) 0.7f else 0.55f).height(18.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    HdSkeletonBlock(modifier = Modifier.fillMaxWidth(0.4f).height(14.dp))
                }
            }
        }
    }
}

/** Stacked card placeholders for dashboard-style screens (summaries, charts). */
@Composable
fun HdCardsSkeleton(modifier: Modifier = Modifier, heights: List<Dp> = listOf(120.dp, 200.dp, 160.dp)) {
    Column(
        modifier = modifier.clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        heights.forEach { height ->
            HdSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height),
                shape = MaterialTheme.shapes.large
            )
        }
    }
}
