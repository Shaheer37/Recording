package com.android.shaheer.recording.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.android.shaheer.recording.MainActivity;
import com.android.shaheer.recording.R;
import com.android.shaheer.recording.utils.SessionManager;
import com.android.shaheer.recording.utils.FilesUtil;
import com.android.shaheer.recording.utils.Recorder;


public class RecordingService extends Service implements Recorder.RecorderTickListener{
    public static final String TAG = RecordingService.class.getSimpleName();

    public static final String ACTION_RESUME = "action.resume";
    public static final String ACTION_PAUSE = "action.pause";
    public static final String ACTION_STOP = "action.stop";
    public static final String ACTION_START= "action.start";

    private final int NOTIFICATION_ID = 101;

    private Recorder mRecorder;
    private ServiceInterface mServiceInterface;
    private TelephonyManager mTelephonyManager;
    private RecordingInterface mRecordingInterface;

    private String mFileName;

    SessionManager sessionManager;

    public RecordingService() {}

    @Override
    public void onCreate() {
        super.onCreate();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mServiceInterface = new ServiceInterface();

        sessionManager = new SessionManager(getApplicationContext());
        mRecorder = new Recorder(
                getApplicationContext(),
                sessionManager.getBitrate(),
                sessionManager.getChannels(),
                this
        );
        mFileName = mRecorder.getFileName();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        mTelephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind()");
        return mServiceInterface;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnBind()");
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null && intent.getAction() != null) {
            performAction(intent.getAction());
        }

        return START_NOT_STICKY;
    }

    private void performAction(String action){
        switch (action) {
            case ACTION_START:
                try{
                    mRecorder.startRecording(mRecorder.getOutputFile());
                    startForeground(NOTIFICATION_ID, setupNotification(Recorder.RecordingStatus.recording));
                    mTelephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
                    sessionManager.setLastRecording(mServiceInterface.getFilePath());
                    if (mRecordingInterface != null) mRecordingInterface.onRecordingStart();
                }catch (Exception e){
                    stopForegroundService(e);
                }
                break;
            case ACTION_RESUME:
                try {
                    mRecorder.resumeRecording();
                    notifyUpdate(Recorder.RecordingStatus.recording);
                    if (mRecordingInterface != null) {
                        mRecordingInterface.onRecordingResume();
                    }
                }catch (Exception e){
                    stopForegroundService(e);
                }
                break;
            case ACTION_PAUSE:
                mRecorder.pauseRecording();
                notifyUpdate(Recorder.RecordingStatus.paused);
                if(mRecordingInterface != null){
                    mRecordingInterface.onRecordingPause();
                }
                break;
            case ACTION_STOP:
                stopForegroundService(null);
                break;
            default:
                stopForegroundService(null);
                break;
        }
    }

    public class ServiceInterface extends Binder {
        public boolean isRecording() {return (mRecorder.getmRecordingStatus() == Recorder.RecordingStatus.recording);}
        public boolean isPaused() {return (mRecorder.getmRecordingStatus() == Recorder.RecordingStatus.paused);}
        public Recorder.RecordingStatus getRecorderStatus(){return mRecorder.getmRecordingStatus();}
        public String getFilePath(){return FilesUtil.getDir(getApplicationContext()) +"/"+ mFileName;}
        public String getDuration(){ return mRecorder.getFormatedDuration(); }
        public void startRecording(){ performAction(ACTION_START); }
        public void pauseRecording(){ performAction(ACTION_PAUSE); }
        public void resumeRecording(){
            performAction(ACTION_RESUME);
        }
        public void stopRecording(){
            performAction(ACTION_STOP);
        }
        public void stopService(){stopForegroundService(null);}
        public void setRecordingInterface(RecordingInterface recordingInterface){
            mRecordingInterface = recordingInterface;
        }
    }

    @Override
    public void onTick(String duration, float amp) {
        if(mRecordingInterface != null){
            mRecordingInterface.onDurationChange(duration, amp);
        }
    }

    private PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    try{
                        if(mRecorder.getmRecordingStatus() == Recorder.RecordingStatus.paused){
                            mRecorder.resumeRecording();
                            notifyUpdate(Recorder.RecordingStatus.recording);
                            if(mRecordingInterface != null){
                                mRecordingInterface.onRecordingResume();
                            }
                        }
                    }catch (Exception e){
                        stopForegroundService(e);
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_RINGING:
                    if(mRecorder.getmRecordingStatus() != Recorder.RecordingStatus.paused){
                        mRecorder.pauseRecording();
                        notifyUpdate(Recorder.RecordingStatus.paused);
                        if(mRecordingInterface != null){
                            mRecordingInterface.onRecordingPause();
                        }
                    }
                    break;
            }
        }
    };

    private void stopForegroundService(Exception e) {
        Log.d(TAG, "Stop foreground service.");

        if(mRecorder != null) mRecorder.stopRecording();

        if(mRecordingInterface != null){
            mRecordingInterface.onRecordingStop();
            mRecordingInterface.unbind(e);
        }
        else{
            Log.e(TAG, "interface null.");
        }

        stopForeground(true);

        stopSelf();
    }

    private NotificationCompat.Builder getNotificationBuilder(Recorder.RecordingStatus status) {
        createChannel(
                getString(R.string.channel_id),
                getString(R.string.channel_name),
                getString(R.string.channel_description)
        );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.channel_id));

        RemoteViews notificationLayout = new RemoteViews(getApplicationContext().getPackageName(), R.layout.player_notif);

        if(status == Recorder.RecordingStatus.paused){
            notificationLayout.setOnClickPendingIntent(R.id.btn_play_toggle, getActionIntent(ACTION_RESUME, NOTIFICATION_ID, RecordingService.class));
            notificationLayout.setImageViewResource(R.id.btn_play_toggle, R.drawable.ic_record_notif);
        }else{
            notificationLayout.setOnClickPendingIntent(R.id.btn_play_toggle, getActionIntent(ACTION_PAUSE, NOTIFICATION_ID, RecordingService.class));
            notificationLayout.setImageViewResource(R.id.btn_play_toggle, R.drawable.ic_pause_notif);
        }
        notificationLayout.setOnClickPendingIntent(R.id.btn_stop, getActionIntent(ACTION_STOP, NOTIFICATION_ID, RecordingService.class));
        notificationLayout.setTextViewText(R.id.notification_title, mRecorder.getFileName());
        notificationLayout.setTextViewText(R.id.notification_text, status.toString());

        notificationBuilder.setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notif)
                .setOnlyAlertOnce(true)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(notificationLayout)
                .setSound(null);

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notificationBuilder.setContentIntent(pendingIntent);

        return notificationBuilder;
    }

    private Notification setupNotification(Recorder.RecordingStatus status){
        return getNotificationBuilder(status).build();
    }

    private void notifyUpdate(Recorder.RecordingStatus recordingStatus){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, setupNotification(recordingStatus));
    }

    private PendingIntent getActionIntent(String action, int requestId, Class<? extends Service> service) {
        Intent stopIntent = new Intent(this, service);
        stopIntent.setAction(action);
        return PendingIntent.getService(this, requestId, stopIntent, 0);
    }

    private void createChannel(String channelId, String channelName, String channelDescription) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notifManager = (NotificationManager) ContextCompat.getSystemService(getApplicationContext(), NotificationManager.class);

            NotificationChannel notificationChannel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
            );

            notificationChannel.setShowBadge(false);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription(channelDescription);

            notifManager.createNotificationChannel(notificationChannel);
        }
    }

    public interface RecordingInterface{
        void onRecordingStart();
        void onRecordingPause();
        void onRecordingResume();
        void onRecordingStop();
        void unbind(Exception e);
        void onDurationChange(String duration, float amp);
    }
}
