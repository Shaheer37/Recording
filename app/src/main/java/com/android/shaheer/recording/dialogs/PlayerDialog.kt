package com.android.shaheer.recording.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import com.android.shaheer.recording.R
import com.android.shaheer.recording.databinding.DialogPlayerBinding
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.FilesUtil

class PlayerDialog(
        context: Context,
        private val playerDialogListener: PlayerDialogListener
): Dialog(context) {

    public enum class PlayerState{
        Playing, Paused
    }

    private val binding = DialogPlayerBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setBtnEvents()
    }

    override fun show() {
        super.show()
        setCanceledOnTouchOutside(false)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.decorView?.background?.alpha = 0
    }

    override fun onBackPressed() {
        playerDialogListener.stopPlayer()
        super.onBackPressed()
    }

    private fun setBtnEvents(){
        binding.btnStop.setOnClickListener { playerDialogListener.stopPlayer() }
        binding.btnPlay.setOnClickListener { playerDialogListener.onPlayToggle() }

        binding.sbProgress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    Log.d("PlayerDialog", progress.toString())
                    playerDialogListener.seekPlayer(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun setCurrentPlayingTrack(recordItem: RecordItem){
        binding.tvTitle.text = recordItem.recordAddress
        binding.tvDuration.text = recordItem.recordDuration
    }

    fun pause(){
        binding.btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_play_player))
    }

    fun play(){
        binding.btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_pause_player))
    }

    fun durationUpdate(position: Double, duration: Double) {
        binding.sbProgress.progress = ((position/duration)*100).toInt()
        binding.tvProgress.text = FilesUtil.formatDuration(position.toLong())
    }

    interface PlayerDialogListener{
        fun onPlayToggle()
        fun seekPlayer(position: Int)
        fun stopPlayer()
    }
}