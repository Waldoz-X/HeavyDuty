package com.example.heavyduty.core.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * Shared-element transitions between screens ("container transform"): an element present on
 * both screens — an exercise's image in the list and on its detail page — grows from one
 * place to the other instead of the screens just sliding.
 *
 * The app wraps its NavHost in a SharedTransitionLayout and provides
 * [LocalSharedTransitionScope]; each destination that takes part provides its own
 * [LocalNavAnimatedVisibilityScope]. Elements then opt in with [hdSharedBounds].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Matches this element with the one on the other screen using the same [key]. Its bounds
 * morph between the two while their contents crossfade, clipped to [clipShape].
 *
 * A no-op outside a shared transition (previews, dialogs, screens that don't provide the
 * scopes), so components can use it unconditionally.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.hdSharedBounds(key: Any, clipShape: Shape): Modifier {
    val transitionScope = LocalSharedTransitionScope.current ?: return this
    val visibilityScope = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(transitionScope) {
        this@hdSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = visibilityScope,
            clipInOverlayDuringTransition = OverlayClip(clipShape),
        )
    }
}
