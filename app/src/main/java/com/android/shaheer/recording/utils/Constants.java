package com.android.shaheer.recording.utils;

import android.Manifest;

public class Constants {

    public static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO , Manifest.permission.READ_PHONE_STATE};

    public class Audio {
        public static final String FILE_EXT_M4A = "m4a";

        public static final int SAMPLE_RATE_441 = 44100;

        public static final int BIT_RATE_64K = 64000;
        public static final int BIT_RATE_96K = 96000;
        public static final int BIT_RATE_128K = 128000;
        public static final int BIT_RATE_192K = 192000;
        public static final int BIT_RATE_265K = 256000;

    }
}
