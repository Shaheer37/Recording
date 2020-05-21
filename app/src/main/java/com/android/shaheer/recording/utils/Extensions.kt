package com.android.shaheer.recording.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.android.shaheer.recording.R
import com.google.android.material.snackbar.Snackbar

fun Context.showToast(stringResource: Int, duration: Int = Toast.LENGTH_SHORT){
    val toast = Toast.makeText(this, getString(stringResource), duration)
    toast.setGravity(Gravity.CENTER, 0,0)
    toast.show()
}

fun View.makeSnackBar(msg: String, length: Int): Snackbar? {
    val snackbar = Snackbar.make(this, msg, length)

    val typedValue = TypedValue()
    this.context.theme.resolveAttribute(android.R.attr.colorPrimaryDark, typedValue, true)
    snackbar.setBackgroundTint(typedValue.data)

    snackbar.isGestureInsetBottomIgnored = true

    return snackbar
}

fun NotificationManager.createChannel(channelId: String, channelName: String, channelDescription: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationChannel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        .apply {
            setShowBadge(false)
        }

        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.BLUE
        notificationChannel.enableVibration(true)
        notificationChannel.description = channelDescription

        createNotificationChannel(notificationChannel)

    }
}