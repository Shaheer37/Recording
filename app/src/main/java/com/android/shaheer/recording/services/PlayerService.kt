package com.android.shaheer.recording.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.android.shaheer.recording.MainActivity
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.Player
import com.android.shaheer.recording.utils.createChannel
import java.util.concurrent.TimeUnit

class PlayerService : Service(), Player.PlayerEventListener {
    companion object{
        private const val TAG = "PlayerService"
        const val ACTION_RESUME = "action.resume"
        const val ACTION_PAUSE = "action.pause"
        const val ACTION_STOP = "action.stop"
        const val ACTION_START = "action.start"

        private const val NOTIFICATION_ID = 102

        const val TRACKS = "tracks"
        const val POSITION = "position"
    }

    private var isForeground = false

    private lateinit var tracks: List<RecordItem>
    private var playingTrackPosition: Int = 0

    private val player = Player(this)

    private var playerListener: PlayerListener? = null

    private val playerInterface = PlayerInterface()

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind(intent: $intent)")
        if (intent.action != null) {
            performAction(intent.action!!, intent)
        }
        return playerInterface
    }

    inner class PlayerInterface: Binder() {
        fun play(){
            if(::tracks.isInitialized && tracks.isNotEmpty()) {
                try {
                    playTrackAtIndex(playingTrackPosition)
                } catch (e:Exception) {
                    stopPlayerService(e)
                }
            }else stopPlayerService(Exception(getString(R.string.empty_tracks_exception)))
        }
        fun setPlayerListener(playerListener: PlayerListener?){
            this@PlayerService.playerListener = playerListener
        }
        fun getPlayingTrack(): RecordItem = tracks[playingTrackPosition]
        fun getTrackPosition() = player.trackPlayedLength
        fun getTrackDuration() = player.trackDuration
        fun isPlaying() = player.isPlaying
        fun isPaused() = player.isPaused
        fun togglePlay(){
            if(!isPlaying() && player.resume()) playerListener?.resume()
            else if(player.pause()) playerListener?.pause()
            notifyUpdate(player.playingStatus)
        }
        fun seek(position: Int) = player.seek(position.toDouble())
        fun stopPlayer() = stopPlayerService(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action != null) {
            performAction(intent.action!!, intent)
        }
        return START_NOT_STICKY
    }

    private fun performAction(action: String, intent: Intent){
        when(action){
            ACTION_START -> {
                val tracks = intent.extras?.getParcelableArrayList<RecordItem>(TRACKS)
                playingTrackPosition = intent.extras?.getInt(POSITION)?:0
                tracks?.let{ this.tracks = it }
            }
            ACTION_STOP -> {
                stopPlayerService(null)
            }
            ACTION_RESUME -> {
                if(!player.isPlaying && player.resume()){
                    playerListener?.resume()
                    notifyUpdate(player.playingStatus)
                }
            }
            ACTION_PAUSE -> {
                if(player.isPlaying && player.pause()){
                    playerListener?.pause()
                    notifyUpdate(player.playingStatus)
                }
            }
        }
    }

    private fun stopPlayerService(e:Exception?) {
        Log.d(RecordingService.TAG, "stopService()")

        playerListener?.unbind(e)
        player.stop()
        stopForeground(true)
        stopSelf()
    }

    private fun playTrackAtIndex(position: Int){
        if(playingTrackPosition != position || player.isNotPlaying) {
            playingTrackPosition = position
            player.play("${FilesUtil.getDir(applicationContext)}/${tracks[position].recordAddress}.${tracks[position].recordExtension}")
        }
    }

    override fun onTrackStarted(duration: Long) {
//        btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_pause_player))
        if(isForeground){
            formatDuration(duration)
        }else{
            startForeground(NOTIFICATION_ID, setupNotification(player.playingStatus))
        }
    }

    override fun onTrackCompleted() {
        stopPlayerService(null)
    }

    override fun onDurationUpdate(position: Double, duration: Double) {
        Log.d(TAG, "${((position/duration)*100).toInt()}")
        playerListener?.onDurationUpdate(position, duration)
    }

    fun formatDuration(duration: Long): String{
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration)
        return if(hours>0) String.format("%02d:%02d:%02d", hours, minutes, seconds)
        else String.format("%02d:%02d", minutes, seconds)
    }

    private fun getNotificationBuilder(status: Player.PlayingStatus): NotificationCompat.Builder {

        val notifManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java) as NotificationManager
        notifManager.createChannel(
                getString(R.string.channel_id),
                getString(R.string.channel_name),
                getString(R.string.channel_description)
        )

        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.channel_id))

        val notificationLayout = RemoteViews(applicationContext.packageName, R.layout.player_notif)

        if(status != Player.PlayingStatus.playing){
            notificationLayout.setOnClickPendingIntent(R.id.btn_play_toggle, getActionIntent(ACTION_RESUME, NOTIFICATION_ID, PlayerService::class.java))
            notificationLayout.setImageViewResource(R.id.btn_play_toggle, R.drawable.ic_play_notif)
        }else{
            notificationLayout.setOnClickPendingIntent(R.id.btn_play_toggle, getActionIntent(ACTION_PAUSE, NOTIFICATION_ID, PlayerService::class.java))
            notificationLayout.setImageViewResource(R.id.btn_play_toggle, R.drawable.ic_pause_notif)
        }
        notificationLayout.setOnClickPendingIntent(R.id.btn_stop, getActionIntent(ACTION_STOP, NOTIFICATION_ID, PlayerService::class.java))
        notificationLayout.setTextViewText(R.id.notification_title, tracks[playingTrackPosition].recordAddress)
        notificationLayout.setTextViewText(R.id.notification_text, status.toString())

        notificationBuilder.setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notif)
                .setOnlyAlertOnce(true)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notificationBuilder.setContentIntent(pendingIntent)

        return notificationBuilder
    }

    private fun setupNotification(status: Player.PlayingStatus): Notification? {
        return getNotificationBuilder(status).build()
    }

    private fun notifyUpdate(status: Player.PlayingStatus) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, setupNotification(status))
    }

    private fun getActionIntent(action: String, requestId: Int, service: Class<out Service>): PendingIntent{
        val stopIntent = Intent(this, service)
        stopIntent.action = action
        return PendingIntent.getService(this, requestId, stopIntent, 0)
    }

    interface PlayerListener {
        fun unbind(e:Exception?)
        fun onDurationUpdate(position: Double, duration: Double)
        fun pause()
        fun resume()
    }
}
