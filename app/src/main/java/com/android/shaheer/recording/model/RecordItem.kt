package com.android.shaheer.recording.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RecordItem(
    val recordAddress: String,
    val recordExtension: String,
    val recordDuration: String,
    var isSelected: Boolean = false
): Parcelable {
}

/*
public RecordItem copy(){
        RecordItem record = new RecordItem(this.recordAddress, this.recordExtension, this.recordDuration);
        record.isSelected = this.isSelected;
        return record;
    }
* */