package com.android.shaheer.recording.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.shaheer.recording.R;
import com.android.shaheer.recording.service.RecordingService;
import com.android.shaheer.recording.storage.SessionManager;
import com.android.shaheer.recording.utils.CommonMethods;
import com.android.shaheer.recording.utils.Constants;
import com.android.shaheer.recording.utils.DynamicSineWaveView;
import com.android.shaheer.recording.utils.Player;
import com.android.shaheer.recording.utils.Recorder;
import com.omega_r.libs.OmegaCenterIconButton;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import pub.devrel.easypermissions.EasyPermissions;

public class RecordingActivity extends AppCompatActivity implements
        RecordingService.RecordingInterface,
        Player.PlayerEventListener,
        EasyPermissions.PermissionCallbacks,
        EasyPermissions.RationaleCallbacks
{

    public static final String TAG = RecordingActivity.class.getSimpleName();
    private static final int PERMISSION_INT = 123;

    public enum RecordingStatus {
        initial, recording, playing
    }

    private SessionManager mSessionManager;

    private Player mPlayer;

    private RecordingStatus mRecordingStatus = RecordingStatus.initial;

    private boolean isBound = false;
    private RecordingService.ServiceInterface mServiceInterface;

    private String mRecordedFile;

    @BindView(R.id.view_sine_wave) public DynamicSineWaveView sineWaveView;
    @BindView(R.id.tv_status) public TextView tvRecording;

    @BindView(R.id.rl_initial) public RelativeLayout rlInitial;
    @BindView(R.id.rl_recording) public RelativeLayout rlRecording;
    @BindView(R.id.rl_recorded) public RelativeLayout rlRecorded;
    @BindView(R.id.ll_bottom_options) public LinearLayout llBottomOptions;

    @BindView(R.id.btn_start_recording) public OmegaCenterIconButton btnStartRecording;
    @BindView(R.id.btn_play_last_recording) public OmegaCenterIconButton btnPlayLastRecording;

    @BindView(R.id.tv_record_duration) public TextView tvRecordDuration;
    @BindView(R.id.btn_recording_action) public ImageButton btnRecordAction;
    @BindView(R.id.btn_recording_stop) public ImageButton btnRecordStop;
    @BindView(R.id.tv_recording_action) public TextView tvRecordAction;

    @BindView(R.id.btn_play_recording) public Button btnPlayRecording;
    @BindView(R.id.btn_new_recording) public Button btnNewRecording;

    @BindView(R.id.btn_audio_archive) public Button btnAudioArchive;

    protected ServiceConnection mRecordingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.e(TAG, "onServiceConnected");
            if (RecordingService.class.getName().equals(componentName.getClassName())) {
                isBound = true;
                mServiceInterface = (RecordingService.ServiceInterface) iBinder;
                mServiceInterface.setRecordingInterface(RecordingActivity.this);
                mRecordedFile = mServiceInterface.getFilePath();
                Log.e(TAG, "File: "+ mRecordedFile);
                setRecording();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "onServiceDisconnected");
            if (RecordingService.class.getName().equals(componentName.getClassName())) {
                isBound = false;
                mServiceInterface.setRecordingInterface(null);
                mServiceInterface = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording);
        ButterKnife.bind(this);

        mSessionManager = new SessionManager(this);
        mPlayer = new Player(this, this);

        float stroke = CommonMethods.dipToPixels(this, 2);
        sineWaveView = (DynamicSineWaveView) findViewById(R.id.view_sine_wave);
        sineWaveView.addWave(0.5f, 0.5f, 0, 0, 0); // Fist wave is for the shape of other waves.
        sineWaveView.addWave(0.5f, 2f, 0.5f, getResources().getColor(android.R.color.white), stroke);
        sineWaveView.addWave(0.1f, 2f, 0.7f, getResources().getColor(R.color.lightBlue), stroke);
        sineWaveView.setBaseWaveAmplitudeScale(1);

        if (EasyPermissions.hasPermissions(this, Constants.PERMISSIONS)) {
            btnStartRecording.setEnabled(true);
        } else {
            btnStartRecording.setEnabled(false);
            EasyPermissions.requestPermissions(this, getString(R.string.permission_rationale), PERMISSION_INT, Constants.PERMISSIONS);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        Log.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
        btnStartRecording.setEnabled(true);
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, getString(R.string.on_permission_denied), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRationaleAccepted(int requestCode) { }

    @Override
    public void onRationaleDenied(int requestCode) { }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mPlayer.isPlaying()){
            mPlayer.stop();
            sineWaveView.stopAnimation();
            tvRecording.setText(R.string.recording_completed);
        }

        if (isBound) {
            unBindService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (CommonMethods.isServiceRunning(RecordingService.class, this)) {
            bindService();
        }
        if(mRecordingStatus == RecordingStatus.initial){
            setLastRecordingControls();
        }
    }

    private void bindService() {
        Log.e(TAG, "bindService()");
        Intent intent = new Intent(this, RecordingService.class);
        bindService(intent, mRecordingServiceConnection,
                BIND_AUTO_CREATE);
    }

    private void unBindService() {
        Log.e(TAG, "unBindService()");
        mServiceInterface.setRecordingInterface(null);
        unbindService(mRecordingServiceConnection);
    }

    @OnClick(R.id.btn_start_recording)
    public void onRecordAction() {
        if(mPlayer.isPlaying() || mPlayer.isPaused()){
            mPlayer.stop();
        }
        Intent intent = new Intent(this, RecordingService.class);
        intent.setAction(RecordingService.ACTION_START);
        startService(intent);

        bindService();
    }

    @OnClick(R.id.btn_recording_action)
    public void onRecordingAction() {
        if (mServiceInterface.isPaused()) {
            setRecordingStatus(Recorder.RecordingStatus.recording);
            mServiceInterface.resumeRecording();
        } else {
            setRecordingStatus(Recorder.RecordingStatus.paused);
            mServiceInterface.pauseRecording();
        }
    }

    @OnClick(R.id.btn_recording_stop)
    public void stopRecording() {
        if (mServiceInterface != null) {
            mServiceInterface.stopRecording();
        }
    }

    @OnClick(R.id.btn_play_recording)
    public void playRecording() {
        if(mPlayer.isPlaying()) {
            mPlayer.pause();
        }else if(mPlayer.isPaused()) {
            mPlayer.resume();
        }else{
            if (mPlayer.play(mRecordedFile)) {
                sineWaveView.startAnimation();
                tvRecording.setText(R.string.playing);
            }
        }

    }

    @OnClick(R.id.btn_new_recording)
    public void startNewRecording() {
        setInitial();
    }

    @OnClick(R.id.btn_audio_archive)
    public void openAudioArchives(){
        Intent ListFiles = new Intent(getApplicationContext(), ListFilesActivity.class);
        startActivity(ListFiles);
    }

    @OnClick(R.id.btn_play_last_recording)
    public void playLastRecording(){
        if(mPlayer.isPlaying()) {
            mPlayer.pause();
        }else if(mPlayer.isPaused()) {
            mPlayer.resume();
        }else{
            mPlayer.play(mSessionManager.getLastRecording());
        }
    }

    public void setLastRecordingControls(){
        String lastRecording = mSessionManager.getLastRecording();
        if(lastRecording != null){
            btnPlayLastRecording.setVisibility(View.VISIBLE);
        }else{
            btnPlayLastRecording.setVisibility(View.GONE);
        }
    }

    private void setRecordingStatus(Recorder.RecordingStatus recordingStatus) {
        switch (recordingStatus) {
            case recording:
                if (Build.VERSION.SDK_INT >= 21) {
                    btnRecordAction.setImageDrawable(getDrawable(R.drawable.bg_recording_action_pause));
                } else {
                    btnRecordAction.setImageDrawable(getResources().getDrawable(R.drawable.bg_recording_action_pause));
                }
                tvRecordAction.setText(R.string.pause);
                tvRecording.setText(R.string.recording);
                break;
            case paused:
                if (Build.VERSION.SDK_INT >= 21) {
                    btnRecordAction.setImageDrawable(getDrawable(R.drawable.bg_recording_action_record));
                } else {
                    btnRecordAction.setImageDrawable(getResources().getDrawable(R.drawable.bg_recording_action_record));
                }
                tvRecordAction.setText(R.string.resume);
                tvRecording.setText(R.string.paused);
                break;
            case ended:
                if (Build.VERSION.SDK_INT >= 21) {
                    btnRecordAction.setImageDrawable(getDrawable(R.drawable.bg_recording_action_record));
                } else {
                    btnRecordAction.setImageDrawable(getResources().getDrawable(R.drawable.bg_recording_action_record));
                }
                tvRecordAction.setText(R.string.pause);
                setRecorded();
                break;
        }
    }

    private void setRecording() {
        if(mPlayer.isPlaying() || mPlayer.isPaused()){
            mPlayer.stop();
        }
        rlInitial.setVisibility(View.GONE);
        rlRecorded.setVisibility(View.GONE);
        llBottomOptions.setVisibility(View.GONE);
        rlRecording.setVisibility(View.VISIBLE);

        setRecordingStatus(Recorder.RecordingStatus.recording);
        tvRecording.setText(R.string.recording);

        sineWaveView.setVisibility(View.VISIBLE);
        sineWaveView.startAnimation();

        mRecordingStatus = RecordingStatus.recording;
    }

    private void setInitial() {
        if(mPlayer.isPlaying() || mPlayer.isPaused()){
            mPlayer.stop();
        }
        rlRecording.setVisibility(View.GONE);
        rlRecorded.setVisibility(View.GONE);
        rlInitial.setVisibility(View.VISIBLE);
        llBottomOptions.setVisibility(View.VISIBLE);

        tvRecording.setText(R.string.start_recording);

        sineWaveView.setVisibility(View.INVISIBLE);
        sineWaveView.stopAnimation();
        mRecordingStatus = RecordingStatus.initial;

        setLastRecordingControls();
    }

    private void setRecorded() {
        if(mPlayer.isPlaying() || mPlayer.isPaused()){
            mPlayer.stop();
        }
        rlRecording.setVisibility(View.GONE);
        rlInitial.setVisibility(View.GONE);
        rlRecorded.setVisibility(View.VISIBLE);
        llBottomOptions.setVisibility(View.VISIBLE);

        tvRecording.setText(R.string.recording_completed);

        sineWaveView.setVisibility(View.VISIBLE);
        sineWaveView.stopAnimation();

        mRecordingStatus = RecordingStatus.playing;
    }

    @Override
    public void onRecordingPause() {
        setRecordingStatus(Recorder.RecordingStatus.paused);
    }

    @Override
    public void onRecordingResume() {
        setRecordingStatus(Recorder.RecordingStatus.recording);
    }

    @Override
    public void onRecordingStop() {
        setRecordingStatus(Recorder.RecordingStatus.ended);
    }

    @Override
    public void onDurationChange(String duration) {
        tvRecordDuration.setText(duration);
    }

    @Override
    public void onTrackCompleted() {
        if(mRecordingStatus == RecordingStatus.playing) {
            sineWaveView.stopAnimation();
            tvRecording.setText(R.string.recording_completed);
        }
    }

    @Override
    public void unbind() {
        Log.e(TAG, "unbind()");
        isBound = false;
        mServiceInterface = null;
        unbindService(mRecordingServiceConnection);
    }
}
