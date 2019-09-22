package com.android.shaheer.recording.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    SharedPreferences mPrefs;

    SharedPreferences.Editor mEditor;

    Context context;

    public static final String LAST_RECORDING = "lastRecording";

    public static final String PREFS_NAME = "com.android.shaheer.recording";

    public SessionManager(Context context)
    {
        this.context = context;

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        mEditor = mPrefs.edit();
    }

    public void setLastRecording(String file){
        mEditor.putString(LAST_RECORDING, file);
        mEditor.apply();
    }

    public String getLastRecording(){
        return mPrefs.getString(LAST_RECORDING, null);
    }
}
