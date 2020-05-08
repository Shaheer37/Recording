package com.android.shaheer.recording.utils;

import android.Manifest;

public class Constants {

    public static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO , Manifest.permission.READ_PHONE_STATE};

    public class AudioFormat{
        public static final String FILE_EXT_M4A = "m4a";
    }
}
