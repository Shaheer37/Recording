package com.android.shaheer.recording.editrecordings

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.Event
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.Player
import com.android.shaheer.recording.utils.SessionManager
import java.io.File
import java.util.concurrent.TimeUnit

class ViewRecordsViewModel(private val sessionManager: SessionManager): ViewModel(){

    private var _recordings = MutableLiveData<List<RecordItem>>()
    val recordings: LiveData<List<RecordItem>> = _recordings

    private var _selectedRecordings = MutableLiveData(0)
    val selectedRecordings: LiveData<Int> = _selectedRecordings

    private var _renameItem = MutableLiveData<Event<String>>()
    val renameItem: LiveData<Event<String>> = _renameItem

    private var _getRecordings = MutableLiveData<Event<Boolean>>()
    val getRecordings: LiveData<Event<Boolean>> = _getRecordings

    private var _showNameAlreadyExistsToast = MutableLiveData<Event<Boolean>>()
    val showNameAlreadyExistsToast: LiveData<Event<Boolean>> = _showNameAlreadyExistsToast

    private val player = Player(Player.onTrackCompletedListener {

    })

    private var playingTrackIndex: Int? = null


    fun onRecordItemSelected(position: Int) = _recordings.value?.let {
        val recordingList = it.toMutableList()
        val record = it[position].copy()
        record.isSelected = !record.isSelected

        _selectedRecordings.value?.let { count->
            if(record.isSelected) _selectedRecordings.value = count + 1
            else _selectedRecordings.value = count - 1
        }

        recordingList[position] = record
        _recordings.value = recordingList
    }

    fun onRecordItemClicked(position: Int) = _recordings.value?.let {
        if(it[position].isSelected) onRecordItemSelected(position)
    }

    fun renameSelectedItem(){
        _recordings.value?.find { it.isSelected }?.let {
            _renameItem.value = Event(it.recordAddress)
        }
    }

    fun getRecordingsFromFiles(context: Context, forced: Boolean = false){
        val recordingList = mutableListOf<RecordItem>()
        val directory = File(FilesUtil.getDir(context))
        val list = directory.listFiles()
        for (i in list.indices) {
            val name = list[i].getName()
            if (name.contains("m4a")) {
                val audioFilename = name.split(".m4a").first()
                val metaRetriever = MediaMetadataRetriever()
                metaRetriever.setDataSource(directory.absolutePath + "/" + name)

                // convert duration to minute:seconds
                val duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
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

        if(forced) _recordings.value = recordingList
        else{
            _recordings.value?.let { recordings ->

                val filtered = recordings.fold(hashMapOf<String, RecordItem>()){ map, item ->
                    if(item.isPlaying || item.isSelected) map[item.recordAddress] = item
                    map
                }

                if(filtered.isNotEmpty()){
                    recordingList.map {item ->
                        filtered[item.recordAddress]?.let {
                            item.isPlaying = it.isPlaying
                            item.isSelected = it.isSelected
                        }
                        item
                    }
                }else _recordings.value = recordingList

            }?: run {_recordings.value = recordingList}
        }
    }

    fun renameRecordingFile(context: Context?, newName: String, lastName: String){
//        Log.d("ViewRecordsVM", "renameRecordingFile(context: Context, newName: $newName, lastName: $lastName)")
        _recordings.value?.fold(RenameCheckPair(null, false)) { pair, item ->
            if(item.recordAddress == lastName) pair.sameItem = item
            if(item.recordAddress == newName) pair.sameNameExists = true
            pair
        }?.let { pair ->
            if(pair.sameNameExists){
                _showNameAlreadyExistsToast.value = Event(true)
                return@let
            }
            pair.sameItem?.let {item->
                val fileName = "$newName.m4a"
                val directory = File(FilesUtil.getDir(context))
                val audioFile = File(directory.absolutePath + "/" + item.recordAddress + ".m4a")
                audioFile.renameTo(File(directory.absolutePath + "/" + fileName))
                _selectedRecordings.value?.let { count-> _selectedRecordings.value = count - 1}
                _getRecordings.value = Event(true)
            }
        }
    }
}

data class RenameCheckPair(
    public var sameItem: RecordItem?,
    public var sameNameExists: Boolean
)

@Suppress("UNCHECKED_CAST")
class ViewRecordsViewModelFactory (private val sessionManager: SessionManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (ViewRecordsViewModel(sessionManager) as T)
}