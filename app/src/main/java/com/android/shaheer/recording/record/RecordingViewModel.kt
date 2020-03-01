package com.android.shaheer.recording.record

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import com.android.shaheer.recording.utils.Event
import com.android.shaheer.recording.utils.Player
import com.android.shaheer.recording.utils.Recorder
import com.android.shaheer.recording.utils.SessionManager

public class RecordingViewModel(val sessionManager: SessionManager)
    : ViewModel(), RecordingService.RecordingInterface, Player.PlayerEventListener {
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

    private val _showLastRecordingButton = MutableLiveData<Event<Boolean>>()
    public val showLastRecordingButton: LiveData<Event<Boolean>> = _showLastRecordingButton

    private var serviceInterface: RecordingService.ServiceInterface? = null

    private var recordingFile: String? = null
    private val player = Player(this)

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
                _isServiceBound.value = Event(true)
                serviceInterface?.setRecordingInterface(null)
                serviceInterface = null
            }
        }
    }


    fun setStateInitial(){
        if(_state.value == RecordingFragment.RecordingStatus.playing) player.stop()
        _state.value = RecordingFragment.RecordingStatus.initial
    }
    fun setStateRecording(){
        if(_state.value == RecordingFragment.RecordingStatus.playing) player.stop()
        _state.value = RecordingFragment.RecordingStatus.recording
        _recordingState.value = Event(Recorder.RecordingStatus.recording)
    }
    fun setStatePlaying(){
        _state.value = RecordingFragment.RecordingStatus.playing
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

    fun playRecording() {
        when{
            player.isPlaying -> player.pause()
            player.isPaused -> player.resume()
            else -> recordingFile?.let{recordingFile
                if (player.play(it)) setStatePlaying()
            }?: run{
                sessionManager.lastRecording?.let {
                    if (player.play(it)) setStatePlaying()
                }
            }
        }
    }

    fun setLastRecordingControls() {
        val state = _state.value?:RecordingFragment.RecordingStatus.initial
        if(state == RecordingFragment.RecordingStatus.initial){
            sessionManager.lastRecording?.let {
                _showLastRecordingButton.value = Event(true)
            }?: run {
                _showLastRecordingButton.value = Event(false)
            }
        }
    }

    override fun onTrackCompleted() {
        setStateInitial()
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
            if(it.peekContent())
            _isServiceBound.value = Event(false)
            serviceInterface = null
            _unbindService.value = Event(recordingServiceConnection)
        }
    }

    override fun onDurationChange(duration: String?) {
        _duration.value = duration
    }
}

@Suppress("UNCHECKED_CAST")
class RecordingViewModelFactory (private val sessionManager: SessionManager) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (RecordingViewModel(sessionManager) as T)
}