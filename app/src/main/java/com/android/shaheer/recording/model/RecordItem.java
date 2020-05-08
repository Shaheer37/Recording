package com.android.shaheer.recording.model;

import com.android.shaheer.recording.utils.Player;

public class RecordItem {

    private String recordAddress;
    public String getRecordAddress() {
        return recordAddress;
    }

    private String recordExtension;
    public String getRecordExtension() { return recordExtension; }

    private String recordDuration;
    public String getRecordDuration() {
        return recordDuration;
    }

    private boolean isSelected = false;
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }


    public RecordItem(String recordAddress, String recordExtension, String recordDuration) {
        this.recordAddress = recordAddress;
        this.recordExtension = recordExtension;
        this.recordDuration = recordDuration;
    }

    public RecordItem copy(){
        RecordItem record = new RecordItem(this.recordAddress, this.recordExtension, this.recordDuration);
        record.isSelected = this.isSelected;
        return record;
    }
}
