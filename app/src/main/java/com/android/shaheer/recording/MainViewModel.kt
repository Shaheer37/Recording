package com.android.shaheer.recording

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.services.PlayerService
import com.android.shaheer.recording.utils.Event

class MainViewModel: ViewModel(), PlayerService.PlayerListener {

    companion object{
        private const val TAG = "MainViewModel"
    }

    private val _checkAllPermissions = MutableLiveData<Event<Boolean>>()
    val checkAllPermissions: LiveData<Event<Boolean>> = _checkAllPermissions

    private val _hasAllPermissions = MutableLiveData<Event<Boolean>>()
    val hasAllPermissions: LiveData<Event<Boolean>> = _hasAllPermissions

    private val _checkStoragePermission = MutableLiveData<Event<Boolean>>()
    val checkStoragePermission: LiveData<Event<Boolean>> = _checkStoragePermission

    private val _hasStoragePermission = MutableLiveData<Event<Boolean>>()
    val hasStoragePermission: LiveData<Event<Boolean>> = _hasStoragePermission

    fun allPermissionsGranted(status: Boolean){
        _hasAllPermissions.value = Event(status)
    }

    fun checkPermissions(){
        _checkAllPermissions.value = Event(true)
    }

    fun storagePermissionGranted(status: Boolean){
        _hasStoragePermission.value = Event(status)
    }

    fun checkStoragePermission(){
        _checkStoragePermission.value = Event(true)
    }

    private var _playRecord = MutableLiveData<Event<Pair<Int, ArrayList<RecordItem>>>>()
    val playRecord: LiveData<Event<Pair<Int, ArrayList<RecordItem>>>> = _playRecord

    private val _isPlayingServiceBound = MutableLiveData<Event<Boolean>>()
    public val isPlayingServiceBound: LiveData<Event<Boolean>> = _isPlayingServiceBound

    private val _playerDurationUpdate = MutableLiveData<Pair<Double, Double>>()
    public val playerDurationUpdate: LiveData<Pair<Double, Double>> = _playerDurationUpdate

    private val _playerState = MutableLiveData<Event<PlayerDialog.PlayerState>>()
    public val playerState: LiveData<Event<PlayerDialog.PlayerState>> = _playerState

    private val _bindService = MutableLiveData<Event<ServiceConnection>>()
    public val bindService: LiveData<Event<ServiceConnection>> = _bindService

    private val _currentPlayingTrack = MutableLiveData<Event<RecordItem>>()
    public val currentPlayingTrack: LiveData<Event<RecordItem>> = _currentPlayingTrack

    private var serviceInterface: PlayerService.PlayerInterface? = null

    val playerServiceConnection: PlayerServiceConnection = PlayerServiceConnection()

    override fun unbind() {
        Log.d(TAG, "unbind()")
        _isPlayingServiceBound.value = Event(false)
        serviceInterface?.setPlayerListener(null)
        serviceInterface = null
    }

    override fun onDurationUpdate(position: Double, duration: Double) {
        _playerDurationUpdate.value = Pair(position, duration)
    }

    override fun pause() {
        _playerState.value = Event(PlayerDialog.PlayerState.Paused)
    }

    override fun resume() {
        _playerState.value = Event(PlayerDialog.PlayerState.Playing)
    }

    fun getPlayerstatus() = serviceInterface?.run{
        _currentPlayingTrack.value = Event(getPlayingTrack())
        val duration = getTrackDuration()
        val position = getTrackPosition()
        if(duration>0 && position>0){
            _playerDurationUpdate.value = Pair(position.toDouble(), duration.toDouble())
        }
        if(isPlaying()) _playerState.value = Event(PlayerDialog.PlayerState.Playing)
        if(isPaused()) _playerState.value = Event(PlayerDialog.PlayerState.Paused)
    }

    fun playRecord(record: Pair<Int, ArrayList<RecordItem>>){
        _playRecord.value = Event(record)
    }

    fun bindService(){
        _bindService.value = Event(playerServiceConnection)
    }

    fun onPlayToggle() {
        serviceInterface?.togglePlay()
    }

    fun seekPlayer(position: Int) {
        serviceInterface?.seek(position)
    }

    fun stopPlayer() {
        serviceInterface?.stopPlayer()
    }

    inner class PlayerServiceConnection: ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            Log.e(TAG, "onServiceConnected")
            if (PlayerService::class.java.name == componentName.className) {
                serviceInterface = iBinder as PlayerService.PlayerInterface
                serviceInterface?.setPlayerListener(this@MainViewModel)
                _isPlayingServiceBound.value = Event(true)
            }
        }
        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.e(TAG, "onServiceDisconnected")
            if (PlayerService::class.java.name == componentName.className) {
                _isPlayingServiceBound.value = Event(false)
                serviceInterface?.setPlayerListener(null)
                serviceInterface = null
            }
        }
    }
}
@Suppress("UNCHECKED_CAST")
class MainViewModelFactory () : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (MainViewModel() as T)
}