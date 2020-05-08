package com.android.shaheer.recording.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

public class Recorder {
    private static final String TAG = Recorder.class.getSimpleName();

    private static final int HOUR_IN_MILLISECONDS = 60000;
    private static final int SECOND_IN_MILLISECONDS = 1000;
    private static final int HOUR_IN_MINUTES = 60;

    private static final int MAX_SUPPORTED_AMPLITUDE = 32767;

    private static final short DELAY_MILLI = 100;

    public enum RecordingStatus{
        initiated, recording, paused, ended
    }

    private Context mContext;

    private MediaRecorder mRecorder;

    private RecordingStatus mRecordingStatus = RecordingStatus.initiated;
    public RecordingStatus getmRecordingStatus() {return mRecordingStatus;}

    private long mStartTime = 0;

    private File mOutputFile;
    public File getOutputFile() {return mOutputFile;}

    private FilesUtil filesUtil;

    private static DecimalFormat df = new DecimalFormat("0.00");

    private Handler mHandler = new Handler();
    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            tick();
            mHandler.postDelayed(mTickExecutor,DELAY_MILLI);
        }
    };

    private RecorderTickListener mTickListener;


    public Recorder(Context context, RecorderTickListener tickListener) {

        filesUtil = new FilesUtil();

        mContext = context;
        mOutputFile = filesUtil.getFile(context, null, Constants.AudioFormat.FILE_EXT_M4A);
        mTickListener = tickListener;
    }

    public String getFileName() {
        return mOutputFile.getName();
    }

    public boolean startRecording(File outputFile) {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setAudioEncodingBitRate(64000);
        mRecorder.setAudioSamplingRate(16000);
        outputFile.getParentFile().mkdirs();
        mRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            mRecorder.prepare();
            mRecorder.start();

            mHandler.postDelayed(mTickExecutor, DELAY_MILLI);
            mRecordingStatus = RecordingStatus.recording;
            Log.d(TAG,"started recording to "+outputFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "prepare() failed "+e.getMessage());
            return false;
        } catch (IllegalStateException e){
            Log.e(TAG, "prepare() failed "+e.getMessage());
            return false;
        }
        return true;
    }

    public void pauseRecording(){
        Log.e(TAG, "pauseRecording()");
        if(mRecordingStatus == RecordingStatus.recording) {
            mRecordingStatus = RecordingStatus.paused;
            if (Build.VERSION.SDK_INT >= 24) {
                mHandler.removeCallbacks(mTickExecutor);
                mRecorder.pause();
            } else {
                stopRecording();
            }
        }
    }

    public void resumeRecording(){
        Log.e(TAG, "resumeRecording()");
        if(mRecordingStatus == RecordingStatus.paused) {
            mRecordingStatus = RecordingStatus.recording;
            if (Build.VERSION.SDK_INT >= 24) {
                mHandler.postDelayed(mTickExecutor, DELAY_MILLI);
                mRecorder.resume();
            } else {
                File file = filesUtil.getFile(mContext, null,Constants.AudioFormat.FILE_EXT_M4A);
                filesUtil.addRecordingPiece(file);
                Log.e(TAG, "is Recording started: "+startRecording(file));
            }
        }
    }

    public void stopRecording() {
        if(mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            mHandler.removeCallbacks(mTickExecutor);
            if (Build.VERSION.SDK_INT < 24) filesUtil.mergeFiles(mContext, mOutputFile, Constants.AudioFormat.FILE_EXT_M4A);
        }
        else{
            Log.e(TAG, "recorder is null");
        }
    }

    private void tick() {
        mStartTime = mStartTime + 100;
//        Log.e(TAG, "StartTime: "+mStartTime+" | "+"minutes: "+((mStartTime  / 60000))+ " | Seconds: "+((mStartTime  / 1000) % 60));
        int minutes = (int) (mStartTime  / HOUR_IN_MILLISECONDS);
        int seconds = (int) (mStartTime  / SECOND_IN_MILLISECONDS) % HOUR_IN_MINUTES;
        String duration = minutes+":"+(seconds < 10 ? "0"+seconds : seconds);
//        Log.e(TAG, duration);

        if (mRecorder != null && mTickListener != null) {
            float maxAmp = (float)mRecorder.getMaxAmplitude();
            float maxAmpPercent = maxAmp/MAX_SUPPORTED_AMPLITUDE;
            Log.d(TAG, "Max Amp: "+maxAmp);
            Log.d(TAG, "Max Amp %: "+maxAmpPercent);

            mTickListener.onTick(duration, maxAmpPercent);
        }
    }

    public interface RecorderTickListener{
        void onTick(String duration, float amp);
    }
}
