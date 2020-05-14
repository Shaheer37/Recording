package com.android.shaheer.recording

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.Player
import java.util.concurrent.TimeUnit

class PlayerDialog(
        context: Context
): Dialog(context) {

    public enum class PlayerState{
        Playing, Paused
    }

    @BindView(R.id.tv_title) lateinit var tvTitle: TextView

    @BindView(R.id.sb_progress) lateinit var sbProgress: SeekBar

    @BindView(R.id.tv_duration) lateinit var tvDuration: TextView

    @BindView(R.id.btn_play) lateinit var btnPlay: ImageButton
    @BindView(R.id.btn_stop) lateinit var btnStop: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = View.inflate(context, R.layout.dialog_player, null)
        ButterKnife.bind(this, view)
        setContentView(view)

        setBtnEvents()
    }

    override fun show() {
        super.show()
        setCanceledOnTouchOutside(false)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.decorView?.background?.alpha = 0
    }

    override fun dismiss() {
        super.dismiss()
    }

    fun setBtnEvents(){
        btnStop.setOnClickListener { dismiss() }
        btnPlay.setOnClickListener {

        }

        sbProgress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    Log.d("PlayerDialog", progress.toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun setCurrentPlayingTrack(recordItem: RecordItem){
        tvTitle.text = recordItem.recordAddress
        tvDuration.text = recordItem.recordDuration
    }

    fun setCurrentPlayingTrackPosition(position: Int){
        sbProgress.progress = position
    }

    fun pause(){
        btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_play_24dp))
    }

    fun play(){
        btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_pause_24dp))
    }

    fun durationUpdate(position: Double, duration: Double) {
        sbProgress.progress = ((position/duration)*100).toInt()
    }
}