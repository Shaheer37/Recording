package com.android.shaheer.recording.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.FilesUtil
import kotlinx.android.synthetic.main.dialog_player.*

class PlayerDialog(
        context: Context,
        private val playerDialogListener: PlayerDialogListener
): Dialog(context) {

    public enum class PlayerState{
        Playing, Paused
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = View.inflate(context, R.layout.dialog_player, null)
        setContentView(view)

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

    fun setBtnEvents(){
        btn_stop.setOnClickListener { playerDialogListener.stopPlayer() }
        btn_play.setOnClickListener { playerDialogListener.onPlayToggle() }

        sb_progress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
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
        tv_title.text = recordItem.recordAddress
        tv_duration.text = recordItem.recordDuration
    }

    fun pause(){
        btn_play.setImageDrawable(context.getDrawable(R.drawable.ic_play_24dp))
    }

    fun play(){
        btn_play.setImageDrawable(context.getDrawable(R.drawable.ic_pause_24dp))
    }

    fun durationUpdate(position: Double, duration: Double) {
        sb_progress.progress = ((position/duration)*100).toInt()
        tv_progress.text = FilesUtil.formatDuration(position.toLong())
    }

    interface PlayerDialogListener{
        fun onPlayToggle()
        fun seekPlayer(position: Int)
        fun stopPlayer()
    }
}