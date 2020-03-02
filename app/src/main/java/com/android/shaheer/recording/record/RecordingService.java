package com.android.shaheer.recording.record;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

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

    private final String CHANNEL_ID = "foreground_service";
    private final String CHANNEL_DESCRIPTION = "This is a channel for a recording service notification.";

    private final int NOTIFICATION_ID = 101;

    private Recorder mRecorder;
    private ServiceInterface mServiceInterface;
    private TelephonyManager mTelephonyManager;
    private RecordingInterface mRecordingInterface;

    private String mFileName;

    public RecordingService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mServiceInterface = new ServiceInterface();
        mRecorder = new Recorder(getApplicationContext(),this);
        mFileName = mRecorder.getFileName();
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTelephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_NONE);
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

        if (intent != null) {
            String action = intent.getAction();
            performAction(action);
        }

        return START_NOT_STICKY;
    }

    private void performAction(String action){
        switch (action) {
            case ACTION_START:
                if(mRecorder.startRecording(mRecorder.getOutputFile())) {
                    startForeground(NOTIFICATION_ID, setupNotification(Recorder.RecordingStatus.recording));
                }
                mTelephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
                break;
            case ACTION_RESUME:
                mRecorder.resumeRecording();
                notifyUpdate(Recorder.RecordingStatus.recording);
                if(mRecordingInterface != null){
                    mRecordingInterface.onRecordingResume();
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
                stopForegroundService();
                if(mRecordingInterface != null){
                    mRecordingInterface.onRecordingStop();
                }
                break;
        }
    }

    public class ServiceInterface extends Binder {
        public boolean isRecording() {return (mRecorder.getmRecordingStatus() == Recorder.RecordingStatus.recording);}
        public boolean isPaused() {return (mRecorder.getmRecordingStatus() == Recorder.RecordingStatus.paused);}
        public String getFilePath(){return FilesUtil.getDir(getApplicationContext()) +"/"+ mFileName;}
        public void pauseRecording(){
            performAction(ACTION_PAUSE);
        }
        public void resumeRecording(){
            performAction(ACTION_RESUME);
        }
        public void stopRecording(){
            performAction(ACTION_STOP);
        }
        public void stopService(){stopForegroundService();}
        public void setRecordingInterface(RecordingInterface recordingInterface){
            mRecordingInterface = recordingInterface;
        }
    }

    @Override
    public void onTick(String duration) {
        if(mRecordingInterface != null){
            mRecordingInterface.onDurationChange(duration);
        }
    }

    private PhoneStateListener phoneListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    if(mRecorder.getmRecordingStatus() == Recorder.RecordingStatus.paused){
                        mRecorder.resumeRecording();
                        notifyUpdate(Recorder.RecordingStatus.recording);
                        if(mRecordingInterface != null){
                            mRecordingInterface.onRecordingResume();
                        }
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

    private void stopForegroundService() {
        Log.d(TAG, "Stop foreground service.");

        if(mRecorder != null){
            SessionManager sessionManager = new SessionManager(getApplicationContext());
            sessionManager.setLastRecording(mServiceInterface.getFilePath());
            mRecorder.stopRecording();
        }

        if(mRecordingInterface != null){
            mRecordingInterface.unbind();
        }
        else{
            Log.e(TAG, "interface null.");
        }

        stopForeground(true);

        stopSelf();
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_DESCRIPTION,
                    NotificationManager.IMPORTANCE_LOW);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        notificationBuilder.setWhen(System.currentTimeMillis());
        notificationBuilder.setSmallIcon(R.drawable.ic_notif);
        Bitmap largeIconBitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        notificationBuilder.setLargeIcon(largeIconBitmap);
        notificationBuilder.setPriority(Notification.PRIORITY_DEFAULT);
        notificationBuilder.setContentIntent(pendingIntent);

        return notificationBuilder;
    }

    private Notification setupNotification(Recorder.RecordingStatus recordingStatus){

        NotificationCompat.Builder notificationBuilder = getNotificationBuilder();

        setNotificationText(notificationBuilder, mRecorder.getFileName(), recordingStatus);
        setNotificationActions(notificationBuilder, recordingStatus);

        return notificationBuilder.build();
    }

    private void notifyUpdate(Recorder.RecordingStatus recordingStatus){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, setupNotification(recordingStatus));

    }

    private void setNotificationText(NotificationCompat.Builder notificationBuilder,
                                     String fileName,
                                     Recorder.RecordingStatus recordingStatus
    ){
        notificationBuilder.setContentTitle("Recording "+fileName);
        notificationBuilder.setContentText(recordingStatus.toString());
    }

    private void setNotificationActions(NotificationCompat.Builder notificationBuilder,
                                        Recorder.RecordingStatus recordingStatus
    ){
        if(recordingStatus == Recorder.RecordingStatus.paused){
            // Add Resume button intent in notification.
            Intent playIntent = new Intent(this, RecordingService.class);
            playIntent.setAction(ACTION_RESUME);
            PendingIntent pendingPlayIntent = PendingIntent.getService(this, 0, playIntent, 0);
            NotificationCompat.Action playAction = new NotificationCompat.Action(android.R.drawable.ic_media_play, "Resume", pendingPlayIntent);
            notificationBuilder.addAction(playAction);
        }
        else{
            // Add Pause button intent in notification.
            Intent pauseIntent = new Intent(this, RecordingService.class);
            pauseIntent.setAction(ACTION_PAUSE);
            PendingIntent pendingPauseIntent = PendingIntent.getService(this, 0, pauseIntent, 0);
            NotificationCompat.Action pauseAction = new NotificationCompat.Action(R.drawable.ic_pause, "Pause", pendingPauseIntent);
            notificationBuilder.addAction(pauseAction);
        }
        // Add Pause button intent in notification.
        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, 0);
        NotificationCompat.Action stopAction = new NotificationCompat.Action(R.drawable.ic_stop, "Stop", pendingStopIntent);
        notificationBuilder.addAction(stopAction);
    }

    public interface RecordingInterface{
        void onRecordingPause();
        void onRecordingResume();
        void onRecordingStop();
        void unbind();
        void onDurationChange(String duration);
    }
}
