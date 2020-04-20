package com.android.shaheer.recording.utils

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import com.android.shaheer.recording.R
import com.google.android.material.snackbar.Snackbar

fun Context.showToast(stringResource: Int, duration: Int = Toast.LENGTH_SHORT){
    val toast = Toast.makeText(this, getString(stringResource), duration)
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