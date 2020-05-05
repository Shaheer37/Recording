package com.android.shaheer.recording.model;

import com.android.shaheer.recording.utils.Player;

public class RecordItem {

    private String recordAddress;
    public String getRecordAddress() {
        return recordAddress;
    }
    public void setRecordAddress(String recordAddress) {
        this.recordAddress = recordAddress;
    }

    private String recordDuration;
    public String getRecordDuration() {
        return recordDuration;
    }
    public void setRecordDuration(String recordDuration) {
        this.recordDuration = recordDuration;
    }

    private boolean isSelected = false;
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }


    public RecordItem(String recordAddress, String recordDuration) {
        this.recordAddress = recordAddress;
        this.recordDuration = recordDuration;
    }

    public RecordItem copy(){
        RecordItem record = new RecordItem(this.recordAddress, this.recordDuration);
        record.isSelected = this.isSelected;
        return record;
    }
}
