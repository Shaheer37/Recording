package com.android.shaheer.recording.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.shaheer.recording.MainActivity
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.FilesUtil
import com.android.shaheer.recording.utils.Player
import com.android.shaheer.recording.utils.Recorder
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
    private var playingTrackPosition: Int = -1

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
        fun setPlayerListener(playerListener: PlayerListener?){
            this@PlayerService.playerListener = playerListener
        }
        fun getPlayingTrack(): RecordItem = tracks[playingTrackPosition]
        fun getPlayerPosition() = player.currentTrackPosition
        fun isPlaying() = player.isPlaying
        fun isPaused() = player.isPaused
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
                val position:Int = intent.extras?.getInt(POSITION)?:0
                tracks?.let{
                    this.tracks = it
                    playTrackAtIndex(position)
                }?: stopSelf()
            }
            ACTION_STOP -> {
                stopService()
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

    private fun stopService() {
        Log.d(RecordingService.TAG, "stopService()")

        playerListener?.unbind()
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
//        btnPlay.setImageDrawable(context.getDrawable(R.drawable.ic_pause_24dp))
        if(isForeground){
            formatDuration(duration)
        }else{
            startForeground(NOTIFICATION_ID, setupNotification(player.playingStatus))
        }
    }

    override fun onTrackCompleted() {
        stopService()
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

    private fun getNotificationBuilder(): NotificationCompat.Builder {
        val notificationBuilder: NotificationCompat.Builder
        notificationBuilder = if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(getString(R.string.channel_id),
                    getString(R.string.channel_description),
                    NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
            NotificationCompat.Builder(this, getString(R.string.channel_id))
        } else {
            NotificationCompat.Builder(this)
        }
        notificationBuilder.priority = Notification.PRIORITY_DEFAULT
        notificationBuilder.setWhen(System.currentTimeMillis())

        notificationBuilder.setSmallIcon(R.drawable.ic_notif)
        val largeIconBitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
        notificationBuilder.setLargeIcon(largeIconBitmap)

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notificationBuilder.setContentIntent(pendingIntent)

        return notificationBuilder
    }

    private fun setupNotification(status: Player.PlayingStatus): Notification? {
        val notificationBuilder = getNotificationBuilder()
        setNotificationText(notificationBuilder, tracks[playingTrackPosition].recordAddress, status)
        setNotificationActions(notificationBuilder, status)
        return notificationBuilder.build()
    }

    private fun notifyUpdate(status: Player.PlayingStatus) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, setupNotification(status))
    }

    private fun setNotificationText(notificationBuilder: NotificationCompat.Builder,
                                    fileName: String,
                                    status: Player.PlayingStatus
    ) {
        notificationBuilder.setContentTitle("Playing $fileName")
        notificationBuilder.setContentText(status.toString())
    }

    private fun setNotificationActions(notificationBuilder: NotificationCompat.Builder,
                                       status: Player.PlayingStatus
    ) {
        if (status == Player.PlayingStatus.paused) {
            // Add Resume button intent in notification.
            val playIntent = Intent(this, PlayerService::class.java)
            playIntent.action = ACTION_RESUME
            val pendingPlayIntent = PendingIntent.getService(this, NOTIFICATION_ID, playIntent, 0)
            val playAction = NotificationCompat.Action(android.R.drawable.ic_media_play, "Resume", pendingPlayIntent)
            notificationBuilder.addAction(playAction)
        } else {
            // Add Pause button intent in notification.
            val pauseIntent = Intent(this, PlayerService::class.java)
            pauseIntent.action = ACTION_PAUSE
            val pendingPauseIntent = PendingIntent.getService(this, NOTIFICATION_ID, pauseIntent, 0)
            val pauseAction = NotificationCompat.Action(R.drawable.ic_pause, "Pause", pendingPauseIntent)
            notificationBuilder.addAction(pauseAction)
        }
        // Add Pause button intent in notification.
        val stopIntent = Intent(this, PlayerService::class.java)
        stopIntent.action = ACTION_STOP
        val pendingStopIntent = PendingIntent.getService(this, NOTIFICATION_ID, stopIntent, 0)
        val stopAction = NotificationCompat.Action(R.drawable.ic_stop, "Stop", pendingStopIntent)
        notificationBuilder.addAction(stopAction)
    }

    interface PlayerListener {
        fun unbind()
        fun onDurationUpdate(position: Double, duration: Double)
        fun pause()
        fun resume()
    }
}
