package com.android.shaheer.recording.record

import android.content.ComponentName
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.*
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.services.RecordingService
import com.android.shaheer.recording.utils.Event
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.Recorder
import com.android.shaheer.recording.utils.SessionManager
import java.io.File
import java.lang.Exception

public class RecordingViewModel(val sessionManager: SessionManager)
    : ViewModel(), RecordingService.RecordingInterface {
    companion object {
        private const val TAG = "RecordingViewModel"
    }

    private val _isServiceBound = MutableLiveData<Event<Boolean>>()
    public val isServiceBound: LiveData<Event<Boolean>> = _isServiceBound

    private val _bindService = MutableLiveData<Event<ServiceConnection>>()
    public val bindService: LiveData<Event<ServiceConnection>> = _bindService

    private val _unbindService = MutableLiveData<Event<ServiceConnection>>()
    public val unbindService: LiveData<Event<ServiceConnection>> = _unbindService

    private val _state = MutableLiveData<RecordingFragment.RecordingStatus>()
    public val state: LiveData<RecordingFragment.RecordingStatus> = _state

    private val _recordingState = MutableLiveData<Event<Recorder.RecordingStatus>>()
    public val recordingState: LiveData<Event<Recorder.RecordingStatus>> = _recordingState

    private val _duration = MutableLiveData<String>()
    public val duration: LiveData<String> = _duration

    private val _amplitude = MutableLiveData<Float>()
    public val amplitude: LiveData<Float> = _amplitude

    private val _showErrorToast = MutableLiveData<Event<Int>>()
    public val showErrorToast: LiveData<Event<Int>> = _showErrorToast

    private val _showLastRecordingButton = MutableLiveData<Event<Boolean>>()
    public val showLastRecordingButton: LiveData<Event<Boolean>> = _showLastRecordingButton

    private var _playRecord = MutableLiveData<Event<Pair<Int, ArrayList<RecordItem>>>>()
    val playRecord: LiveData<Event<Pair<Int, ArrayList<RecordItem>>>> = _playRecord

    private var serviceInterface: RecordingService.ServiceInterface? = null

    private var recordingFile: String? = null

    private var recordingServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.e(TAG, "onServiceConnected")
            if (RecordingService::class.java.name == componentName.className) {
                serviceInterface = iBinder as RecordingService.ServiceInterface
                serviceInterface?.setRecordingInterface(this@RecordingViewModel)
                recordingFile = serviceInterface?.filePath
                _isServiceBound.value = Event(true)
                setStateRecording()
            }
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e(TAG, "onServiceDisconnected")
            if (RecordingService::class.java.name == componentName.className) {
                _isServiceBound.value = Event(false)
                serviceInterface?.setRecordingInterface(null)
                serviceInterface = null
            }
        }
    }


    fun setStateInitial(){
        _state.value = RecordingFragment.RecordingStatus.notRecording
    }
    fun setStateRecording(){
        _state.value = RecordingFragment.RecordingStatus.recording
        _recordingState.value = Event(serviceInterface?.recorderStatus?:Recorder.RecordingStatus.recording)
        _showLastRecordingButton.value = Event(false)
    }

    fun bindService(){
        _bindService.value = Event(recordingServiceConnection)
    }

    fun unbindService(){
        unbind()
    }

    fun onRecordingAction(){
        serviceInterface?.let { service ->
            if (service.isPaused) {
                _recordingState.value = Event(Recorder.RecordingStatus.recording)
                service.resumeRecording()
            } else {
                _recordingState.value = Event(Recorder.RecordingStatus.paused)
                service.pauseRecording()
            }
        }
    }

    fun stopRecording() { serviceInterface?.stopRecording()}

    fun playRecording(){
        sessionManager.lastRecording?.let {
            val metaRetriever = MediaMetadataRetriever()
            try{
                val recordItem = FilesUtil.createRecordItem(File(it), metaRetriever)
                if(recordItem != null){
                    _playRecord.value = Event(Pair(0, arrayListOf(recordItem)))
                }
            }catch (e:Exception){
                e.printStackTrace()
            }finally {
                metaRetriever.release()
            }

        }
    }


    fun setLastRecordingControls() {
        val state = _state.value?:RecordingFragment.RecordingStatus.notRecording
        if(state == RecordingFragment.RecordingStatus.notRecording){
            sessionManager.lastRecording?.let {
                _showLastRecordingButton.value = Event(true)
            }?: run {
                _showLastRecordingButton.value = Event(false)
            }
        }
    }

    override fun onRecordingPause() {
        _recordingState.value = Event(Recorder.RecordingStatus.paused)
    }

    override fun onRecordingResume() {
        _recordingState.value = Event(Recorder.RecordingStatus.recording)
    }

    override fun onRecordingStop() {
        _recordingState.value = Event(Recorder.RecordingStatus.ended)
    }

    override fun unbind() {
        _isServiceBound.value?.let {
            if(it.peekContent()) _isServiceBound.value = Event(false)
            serviceInterface = null
            _unbindService.value = Event(recordingServiceConnection)
        }
    }

    override fun onDurationChange(duration: String?, amp: Float) {
        _duration.value = duration
        _amplitude.value = amp
    }
}

@Suppress("UNCHECKED_CAST")
class RecordingViewModelFactory (private val sessionManager: SessionManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (RecordingViewModel(sessionManager) as T)
}