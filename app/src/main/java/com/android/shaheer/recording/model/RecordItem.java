package com.android.shaheer.recording.model;

public class RecordItem {
    private boolean isPlaying = false;
    private String recordAddress;
    private String recordDuration;

    private boolean isSelected = false;


    public RecordItem(String recordAddress, String recordDuration) {
        this.recordAddress = recordAddress;
        this.recordDuration = recordDuration;
    }

    public boolean isSelected() { return isSelected; }

    public void setSelected(boolean selected) { isSelected = selected; }

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

    public RecordItem copy(){
        RecordItem record = new RecordItem(this.recordAddress, this.recordDuration);
        record.isPlaying = this.isPlaying;
        record.isSelected = this.isSelected;
        return record;
    }
}
