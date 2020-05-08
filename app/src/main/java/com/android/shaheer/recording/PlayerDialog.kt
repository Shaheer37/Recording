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
        context: Context,
        var position: Int,
        val tracks: List<RecordItem>
): Dialog(context), Player.PlayerEventListener {

    val player = Player(this)

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
        playTrackAtIndex(position)
    }

    override fun dismiss() {
        player.stop()
        super.dismiss()
    }

    fun setBtnEvents(){
        btnStop.setOnClickListener { dismiss() }
        btnPlay.setOnClickListener {
            when(player.playingStatus){
                Player.PlayingStatus.notPlaying -> playTrackAtIndex(position)
                Player.PlayingStatus.paused -> {
                    player.resume()
                    btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_pause_24dp))
                }
                Player.PlayingStatus.playing ->{
                    player.pause()
                    btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_play_24dp))
                }
                else -> {playTrackAtIndex(position)}
            }
        }

        sbProgress.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser){
                    Log.d("PlayerDialog", progress.toString())
                    player.seek(progress.toDouble())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    fun playTrackAtIndex(position: Int){
        tvTitle.text = tracks[position].recordAddress
        player.play("${FilesUtil.getDir(context)}/${tracks[position].recordAddress}.${tracks[position].recordExtension}")
    }

    override fun onTrackStarted(duration: Long) {
        btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_pause_24dp))
        tvDuration.text = formatDuration(duration)
    }

    override fun onTrackCompleted() {
        dismiss()
    }

    override fun onDurationUpdate(position: Double, duration: Double) {
        sbProgress.progress = ((position/duration)*100).toInt()
    }

    fun formatDuration(duration: Long): String{
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
        return if(hours>0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
            else String.format("%02d:%02d", minutes, seconds)
    }
}