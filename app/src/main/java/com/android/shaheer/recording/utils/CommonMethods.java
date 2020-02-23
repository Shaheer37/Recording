package com.android.shaheer.recording.utils;

import android.app.ActivityManager;
import android.content.Context;

import androidx.core.content.FileProvider;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;

public class CommonMethods {
    public static final String TAG = CommonMethods.class.getSimpleName();

    public static boolean isServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager)context. getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.e(TAG,"Service already running");
                return true;
            }
        }
        Log.e(TAG,"Service not running");
        return false;
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }
}
