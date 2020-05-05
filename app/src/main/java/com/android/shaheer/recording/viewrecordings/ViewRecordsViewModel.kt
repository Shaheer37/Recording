package com.android.shaheer.recording.viewrecordings

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

    private var _playRecord = MutableLiveData<Event<Pair<Int, List<RecordItem>>>>()
    val playRecord: LiveData<Event<Pair<Int, List<RecordItem>>>> = _playRecord

    fun onItemPlayClicked(context:Context, position: Int) = _recordings.value?.let { recordingList->
        _playRecord.value = Event(Pair(position, recordingList))
    }

    fun onRecordItemSelected(position: Int) = _recordings.value?.toMutableList()?.let { recordingList->
        val record = recordingList[position].copy()
        record.isSelected = !record.isSelected

        _selectedRecordings.value?.let { count->
            if(record.isSelected) _selectedRecordings.value = count + 1
            else _selectedRecordings.value = count - 1
        }

        _recordings.value = recordingList.also { it[position] = record }
    }

    fun onRecordItemClicked(position: Int) = _recordings.value?.let {
        if(it[position].isSelected) onRecordItemSelected(position)
    }


    fun renameSelectedItem(){
        _recordings.value?.find { it.isSelected }?.let {
            _renameItem.value = Event(it.recordAddress)
        }
    }

    fun deleteSelectedItems(context: Context?){
        _recordings.value?.forEach {item ->
            if(item.isSelected){
                val directory = File(FilesUtil.getDir(context))
                val fileName = "${directory.absolutePath}/${item.recordAddress}.m4a"

                if (sessionManager.lastRecording != null
                        && fileName.equals(sessionManager.getLastRecording(), ignoreCase = true)
                ) {
                    sessionManager.setLastRecording(null)
                }
                File(fileName).delete()
            }
            _selectedRecordings.value = 0
            _getRecordings.value = Event(true)
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
        recordingList.sortByDescending { it.recordAddress }

        if(forced) _recordings.value = recordingList
        else{
            _recordings.value?.let { recordings ->

                val filtered = recordings.fold(hashMapOf<String, RecordItem>()){ map, item ->
                    if(item.isSelected) map[item.recordAddress] = item
                    map
                }

                if(filtered.isNotEmpty()){
                    recordingList.map {item ->
                        filtered[item.recordAddress]?.let {
                            item.isSelected = it.isSelected
                        }
                        item
                    }
                }else _recordings.value = recordingList

            }?: run {_recordings.value = recordingList}
        }
    }

    fun renameRecordingFile(context: Context?, newName: String, lastName: String){
        if(newName.compareTo(lastName, ignoreCase = true) != 0){
            val itemWithSameName = _recordings.value?.find { it.recordAddress.compareTo(newName, ignoreCase = true) == 0}
            if(itemWithSameName == null){
                val directory = File(FilesUtil.getDir(context))
                val audioFile = File("${directory.absolutePath}/${lastName}.m4a")
                audioFile.renameTo(File("${directory.absolutePath}/$newName.m4a"))
                _selectedRecordings.value?.let { count-> _selectedRecordings.value = count - 1}
                _getRecordings.value = Event(true)
            }else{
                _showNameAlreadyExistsToast.value = Event(true)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class ViewRecordsViewModelFactory (private val sessionManager: SessionManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (ViewRecordsViewModel(sessionManager) as T)
}