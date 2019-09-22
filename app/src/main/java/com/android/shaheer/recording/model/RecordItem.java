package com.android.shaheer.recording.model;

public class RecordItem {
    private boolean isPlaying = false;
    private String recordAddress;
    private String recordDuration;

    public RecordItem(String recordAddress, String recordDuration) {
        this.recordAddress = recordAddress;
        this.recordDuration = recordDuration;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public String getRecordAddress() {
        return recordAddress;
    }

    public String getRecordDuration() {
        return recordDuration;
    }

    public void setRecordAddress(String recordAddress) {
        this.recordAddress = recordAddress;
    }

    public void setRecordDuration(String recordDuration) {
        this.recordDuration = recordDuration;
    }
}
