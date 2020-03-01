package com.android.shaheer.recording.record

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.shaheer.recording.utils.Event

class MainViewModel: ViewModel() {
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
}
@Suppress("UNCHECKED_CAST")
class MainViewModelFactory () : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel> create(modelClass: Class<T>) =
            (MainViewModel() as T)
}