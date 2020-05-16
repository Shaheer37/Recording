package com.android.shaheer.recording.utils;

import android.Manifest;

import com.android.shaheer.recording.dialogs.BitrateItem;
import com.android.shaheer.recording.model.RecordItem;

public class Constants {

    public static final String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO , Manifest.permission.READ_PHONE_STATE};

    public static class Audio {
        public static final String FILE_EXT_M4A = "m4a";

        public static final int SAMPLE_RATE_441 = 44100;

        public static final int CHANNELS_DEFAULT = 2;
        public static final int CHANNELS_MONO = 1;
        public static final int CHANNELS_STEREO = 2;

        public static final int BIT_RATE_DEFAULT = 128000;

        public static final int[] BITRATES = {96000, 128000, 192000, 256000};
        public static final float[] SIZES = {0.7f, 1f, 1.4f, 1.9f};
    }
}
