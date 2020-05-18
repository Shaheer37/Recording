package com.android.shaheer.recording.viewrecordings

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.*
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.services.PlayerService
import com.android.shaheer.recording.utils.Event
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ViewRecordsViewModel(private val sessionManager: SessionManager): ViewModel(){

    companion object{
        private const val TAG = "ViewRecordsViewModel"
    }

    private var _recordings = MutableLiveData<List<RecordItem>>()
    val recordings: LiveData<List<RecordItem>> = _recordings

    private var _selectedRecordings = MutableLiveData(0)
    val selectedRecordings: LiveData<Int> = _selectedRecordings

    private var _renameItem = MutableLiveData<Event<RecordItem>>()
    val renameItem: LiveData<Event<RecordItem>> = _renameItem

    private var _getRecordings = MutableLiveData<Event<Boolean>>()
    val getRecordings: LiveData<Event<Boolean>> = _getRecordings

    private var _showNameAlreadyExistsToast = MutableLiveData<Event<Boolean>>()
    val showNameAlreadyExistsToast: LiveData<Event<Boolean>> = _showNameAlreadyExistsToast

    private var _playRecord = MutableLiveData<Event<Pair<Int, ArrayList<RecordItem>>>>()
    val playRecord: LiveData<Event<Pair<Int, ArrayList<RecordItem>>>> = _playRecord

    fun onItemPlayClicked(context:Context, position: Int) = _recordings.value?.let { recordingList->
        _playRecord.value = Event(Pair(position, recordingList.toCollection(ArrayList())))
    }

    fun onRecordItemSelected(position: Int) = viewModelScope.launch(Dispatchers.IO) {
        _recordings.value?.toMutableList()?.let { recordingList ->
            val record = recordingList[position].copy()
            record.isSelected = !record.isSelected

            _selectedRecordings.value?.let { count ->
                withContext(Dispatchers.Main) {
                    if (record.isSelected) _selectedRecordings.value = count + 1
                    else _selectedRecordings.value = count - 1
                }
            }

            setRecordings(recordingList.also { it[position] = record })
        }
    }

    fun onRecordItemClicked(position: Int) = _recordings.value?.let {
        if(it[position].isSelected) onRecordItemSelected(position)
    }


    fun renameSelectedItem(){
        _recordings.value?.find { it.isSelected }?.let {
            _renameItem.value = Event(it)
        }
    }

    fun deleteSelectedItems(context: Context?) = viewModelScope.launch(Dispatchers.IO){
        _recordings.value?.forEach {item ->
            if(item.isSelected){
                val directory = File(FilesUtil.getDir(context))
                val fileName = "${directory.absolutePath}/${item.recordAddress}.${item.recordExtension}"

                if (sessionManager.lastRecording != null
                        && fileName.equals(sessionManager.lastRecording, ignoreCase = true)
                ) {
                    sessionManager.lastRecording = null
                }
                File(fileName).delete()
            }
        }

        withContext(Dispatchers.Main){
            _selectedRecordings.value = 0
            _getRecordings.value = Event(true)
        }
    }

    fun getRecordingsFromFiles(context: Context, forced: Boolean = false) = viewModelScope.launch(Dispatchers.IO){
        val recordingList = mutableListOf<RecordItem>()
        val directory = File(FilesUtil.getDir(context))
        val list = directory.listFiles()

        val metaRetriever = MediaMetadataRetriever()
        for (i in list.indices) {
            val recordItem = FilesUtil.createRecordItem(list[i], metaRetriever)
            if(recordItem != null){
                recordingList.add(recordItem)
            }
        }
        metaRetriever.release()

        recordingList.sortByDescending { it.recordAddress }

        if(forced) setRecordings(recordingList)
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
                }else setRecordings(recordingList)

            }?: run {setRecordings(recordingList)}
        }
    }

    private suspend fun setRecordings(recordings: List<RecordItem>) = withContext(Dispatchers.Main){
        _recordings.value = recordings
    }

    fun renameRecordingFile(context: Context?, newName: String, recordItem: RecordItem) = viewModelScope.launch(Dispatchers.IO){
        if(newName.compareTo(recordItem.recordAddress, ignoreCase = true) != 0){
            val itemWithSameName = _recordings.value?.find { it.recordAddress.compareTo(newName, ignoreCase = true) == 0}
            if(itemWithSameName == null) {
                val directory = File(FilesUtil.getDir(context))
                val audioFile = File("${directory.absolutePath}/${recordItem.recordAddress}.${recordItem.recordExtension}")
                audioFile.renameTo(File("${directory.absolutePath}/$newName.${recordItem.recordExtension}"))

                withContext(Dispatchers.Main) {
                    _selectedRecordings.value?.let { count -> _selectedRecordings.value = count - 1 }
                    _getRecordings.value = Event(true)
                }
            }else{
                withContext(Dispatchers.Main) { _showNameAlreadyExistsToast.value = Event(true) }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
class ViewRecordsViewModelFactory (private val sessionManager: SessionManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (ViewRecordsViewModel(sessionManager) as T)
}