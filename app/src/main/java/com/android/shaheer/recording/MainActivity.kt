package com.android.shaheer.recording

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.android.shaheer.recording.databinding.ActivityMainBinding
import com.android.shaheer.recording.dialogs.PlayerDialog
import com.android.shaheer.recording.services.PlayerService
import com.android.shaheer.recording.utils.CommonMethods
import com.android.shaheer.recording.utils.Constants
import com.android.shaheer.recording.utils.EventObserver
import com.android.shaheer.recording.utils.showToast
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class MainActivity :
    AppCompatActivity(),
    EasyPermissions.PermissionCallbacks,
    EasyPermissions.RationaleCallbacks,
    PlayerDialog.PlayerDialogListener
{
    companion object{
        private const val TAG = "MainActivity"
        private const val PERMISSION_INT = 123
        private const val STORAGE_PERMISSION_INT = 124
    }

    private lateinit var viewModel: MainViewModel

    private var playerDialog: PlayerDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

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

        viewModel.playRecord.observe(this, EventObserver{

            val intent = Intent(this, PlayerService::class.java)
            intent.action = PlayerService.ACTION_START
            intent.putExtra(PlayerService.POSITION, it.first)
            intent.putParcelableArrayListExtra(PlayerService.TRACKS, it.second)
            startService(intent)

            viewModel.bindService()
        })
        viewModel.isPlayingServiceBound.observe(this, EventObserver{
            Log.d(TAG, "service bound: $it")
            if(it){
                playerDialog = PlayerDialog(this, this)
                playerDialog?.show()

                viewModel.getPlayerstatus()
            }else{
                if(playerDialog?.isShowing == true){
                    playerDialog?.dismiss()
                    playerDialog = null
                }
                unbindService(viewModel.playerServiceConnection)
            }
        })

        viewModel.bindService.observe(this, EventObserver{
            val intent = Intent(this, PlayerService::class.java)
            bindService(intent, it, Context.BIND_AUTO_CREATE)
        })

        viewModel.playerDurationUpdate.observe(this, Observer{
            if(playerDialog?.isShowing == true) playerDialog?.durationUpdate(it.first,it.second)
        })

        viewModel.playerState.observe(this, EventObserver{
            if(playerDialog?.isShowing == true){
                when(it){
                    PlayerDialog.PlayerState.Paused -> playerDialog?.pause()
                    PlayerDialog.PlayerState.Playing -> playerDialog?.play()
                }
            }
        })

        viewModel.currentPlayingTrack.observe(this, EventObserver {
            if(playerDialog?.isShowing == true){
                playerDialog?.setCurrentPlayingTrack(it)
            }
        })

        viewModel.showErrorToast.observe(this, EventObserver{showToast(it)})
    }

    override fun onPause() {
        super.onPause()

        if (CommonMethods.isServiceRunning(PlayerService::class.java, this)) {
            viewModel.unbind(null)
        }
    }

    override fun onResume() {
        super.onResume()
        if (CommonMethods.isServiceRunning(PlayerService::class.java, this)) {
            viewModel.bindService()
        }
    }

    override fun onPlayToggle() = viewModel.onPlayToggle()

    override fun seekPlayer(position: Int) = viewModel.seekPlayer(position)

    override fun stopPlayer() = viewModel.stopPlayer()

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
