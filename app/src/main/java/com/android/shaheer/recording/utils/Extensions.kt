package com.android.shaheer.recording.utils

import android.content.Context
import android.widget.Toast

fun Context.showToast(stringResource: Int, duration: Int = Toast.LENGTH_SHORT){
    val toast = Toast.makeText(this, getString(stringResource), duration)
    toast.show()
}