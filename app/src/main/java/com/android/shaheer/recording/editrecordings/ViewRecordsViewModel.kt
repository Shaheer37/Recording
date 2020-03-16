package com.android.shaheer.recording.editrecordings

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.SessionManager
import java.io.File
import java.util.concurrent.TimeUnit

class ViewRecordsViewModel(private val sessionManager: SessionManager): ViewModel(){

    private var _recordings = MutableLiveData<List<RecordItem>>()
    val recordings: LiveData<List<RecordItem>> = _recordings

    fun onRecordItemSelected(position: Int) = _recordings.value?.let {
        val recordingList = it.toMutableList()
        val record = it[position].copy()
        record.isSelected = !record.isSelected
        recordingList[position] = record
        _recordings.value = recordingList
    }

    fun onRecordItemClicked(position: Int) = _recordings.value?.let {
        if(it[position].isSelected) onRecordItemSelected(position)
    }

    fun getRecordingsFromFiles(context: Context){
        val recordingList = mutableListOf<RecordItem>()
        val directory = File(FilesUtil.getDir(context))
        val list = directory.listFiles()
        for (i in list.indices) {
            val name = list[i].getName()
            if (name.contains("m4a")) {
                val audioFilename = name.split("\\.m4a").first()
                val metaRetriever = MediaMetadataRetriever()
                metaRetriever.setDataSource(directory.absolutePath + "/" + name)

                // convert duration to minute:seconds
                val duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                Log.v("time", duration)
                val dur = java.lang.Long.parseLong(duration)
                val totalTime = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(dur),
                        TimeUnit.MILLISECONDS.toMinutes(dur),
                        TimeUnit.MILLISECONDS.toSeconds(dur) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(dur))
                )
                recordingList.add(RecordItem(audioFilename, totalTime))
                metaRetriever.release()
            }
        }
        _recordings.value = recordingList
    }
}

@Suppress("UNCHECKED_CAST")
class ViewRecordsViewModelFactory (private val sessionManager: SessionManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (ViewRecordsViewModel(sessionManager) as T)
}