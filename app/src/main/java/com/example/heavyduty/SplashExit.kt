package com.example.heavyduty

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.core.splashscreen.SplashScreenViewProvider
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * The logo turns once and grows as the splash gives way to the app, and the splash fades out
 * over the end of the turn. It grows from the size the splash already shows it at, so it
 * doesn't jump when the animation starts. Only the exit can be animated: Android animates the splash icon itself only
 * when it's an animated vector, and the logo is a PNG (as a vector it's too big for aapt2).
 * Animator durations follow the system's animation scale, so "remove animations" skips it.
 */
internal fun SplashScreenViewProvider.spinLogoOut() {
    val logo = runCatching { iconView }.getOrNull()
    if (logo == null) {
        remove()
        return
    }
    val spin = ObjectAnimator.ofFloat(logo, View.ROTATION, 0f, FULL_TURN).apply {
        duration = SPIN_MILLIS
        interpolator = FastOutSlowInInterpolator()
    }
    val grow = ObjectAnimator.ofPropertyValuesHolder(
        logo,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, END_SCALE),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, END_SCALE)
    ).apply {
        duration = SPIN_MILLIS
        interpolator = FastOutSlowInInterpolator()
    }
    val fade = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
        startDelay = SPIN_MILLIS - FADE_MILLIS
        duration = FADE_MILLIS
        interpolator = AccelerateInterpolator()
    }
    AnimatorSet().apply {
        playTogether(spin, grow, fade)
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = remove()
        })
        start()
    }
}

private const val FULL_TURN = 360f
private const val SPIN_MILLIS = 1_600L
private const val FADE_MILLIS = 450L
private const val END_SCALE = 1.8f
