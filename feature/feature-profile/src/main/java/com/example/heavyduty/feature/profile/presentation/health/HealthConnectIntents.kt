package com.example.heavyduty.feature.profile.presentation.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient

private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

/** Health Connect's page in the Play Store (it lands on its setup once installed). */
internal fun Context.openHealthConnectInStore() {
    val listing = "details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"
    try {
        startActivity(Intent(Intent.ACTION_VIEW, "market://$listing".toUri()).setPackage("com.android.vending"))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/$listing".toUri()))
    }
}

/** This app's page in Health Connect (Android 14+), or Health Connect's home before that. */
internal fun Context.openHealthConnectPermissions() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").putExtra(Intent.EXTRA_PACKAGE_NAME, packageName)
    } else {
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
    }
}
