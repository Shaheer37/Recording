package com.android.shaheer.recording

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.android.shaheer.recording.utils.Constants
import com.android.shaheer.recording.utils.EventObserver
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class MainActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks
{
    companion object{
        private const val TAG = "MainActivity"
        private const val PERMISSION_INT = 123
        private const val STORAGE_PERMISSION_INT = 124
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(
                viewModelStore,
                MainViewModelFactory()
        ).get(MainViewModel::class.java)

        viewModel.checkAllPermissions.observe(this, EventObserver{
            checkPermissions()
        })
        viewModel.checkStoragePermission.observe(this, EventObserver{
            checkStoragePermission()
        })
    }

    @AfterPermissionGranted(PERMISSION_INT)
    fun checkPermissions(){
        if (EasyPermissions.hasPermissions(this, *Constants.PERMISSIONS)) {
            viewModel.allPermissionsGranted(true)
        } else {
            viewModel.allPermissionsGranted(false)
            EasyPermissions.requestPermissions(this, getString(R.string.permission_rationale), PERMISSION_INT, *Constants.PERMISSIONS)
        }
    }

    @AfterPermissionGranted(STORAGE_PERMISSION_INT)
    fun checkStoragePermission(){
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            viewModel.storagePermissionGranted(true)
        } else {
            viewModel.storagePermissionGranted(false)
            EasyPermissions.requestPermissions(this, getString(R.string.storage_permission_rationale), STORAGE_PERMISSION_INT, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        Toast.makeText(this, getString(R.string.on_permission_denied), Toast.LENGTH_SHORT).show()
    }

    override fun onRationaleAccepted(requestCode: Int) {}

    override fun onRationaleDenied(requestCode: Int) {}
}
