package com.android.shaheer.recording.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import com.android.shaheer.recording.R;
import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

//code contributed by https://gist.github.com/chathudan/95d9acdd741b2a577483

public class Recorder {
    private static final String TAG = Recorder.class.getSimpleName();

    public static final String DATE_FORMAT = "dd-MMM-yyyy, h:mm:ss a";
    private static final short DELAY_MILLI = 100;
    private static final String MERGED_FILE = "merged.m4a";

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

    private String mOutputDir;
    public String getOutputDir() {return mOutputDir;}

    private String mFileName;
    public String getFileName() {return mFileName;}

    private ArrayList<File> recordPieces;

    private int[] amplitudes = new int[100];
    private int i = 0;

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
        mContext = context;
        mFileName = getRecordName();
        mOutputDir = getDir();
        mOutputFile = getFileDir(null, mFileName);
        recordPieces = new ArrayList<>();
        mTickListener = tickListener;
    }

    public Recorder(Context context, String dir, String fileName, RecorderTickListener tickListener) {
        mContext = context;
        mFileName = (fileName!=null)?fileName:getRecordName();
        mOutputFile = getFileDir(dir, mFileName);
        recordPieces = new ArrayList<>();
        mTickListener = tickListener;
    }

    public boolean startRecording(File outputFile) {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
            mRecorder.setAudioEncodingBitRate(48000);
        } else {
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mRecorder.setAudioEncodingBitRate(64000);
        }
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

    private String getRecordName(){
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return "RECORDING_"+ dateFormat.format(new Date())+ ".m4a";
    }

    private String getDir(){
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"+mContext.getString(R.string.app_name);
    }

    private File getFileDir(String dir, String fileName) {
        File file = new File(
                (dir!=null)
                    ?(dir+"/"+fileName)
                    :(getDir()+"/"+fileName)
        );
        Log.e(TAG, file.getAbsolutePath());
        return file;
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
                recordPieces.add(mOutputFile);
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
                File file = getFileDir(null, getRecordName());
                recordPieces.add(file);
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
            mergeFiles();
        }
        else{
            Log.e(TAG, "recorder is null");
        }
    }

    private boolean mergeFiles(){
        if(recordPieces.size()>1){
            Log.e(TAG, "more files");
            File mergedFile = getFileDir(null, MERGED_FILE);
            if(mergeMediaFiles(true, recordPieces.toArray(new File[recordPieces.size()]), mergedFile.getAbsolutePath())){
                for(File record: recordPieces){
                    record.delete();
                }
                recordPieces = new ArrayList<>();
                mergedFile.renameTo(mOutputFile);
                return true;
            }else{
                return false;
            }

        }else{
            Log.e(TAG, "only one file");
            return true;
        }
    }

    private boolean mergeMediaFiles(boolean isAudio, File sourceFiles[], String targetFile) {
        try {
            String mediaKey = isAudio ? "soun" : "vide";
            List<Movie> listMovies = new ArrayList<>();
            for (File filename : sourceFiles) {
                listMovies.add(MovieCreator.build(filename.getAbsolutePath()));
            }
            List<Track> listTracks = new LinkedList<>();
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    if (track.getHandler().equals(mediaKey)) {
                        listTracks.add(track);
                    }
                }
            }
            Movie outputMovie = new Movie();
            if (!listTracks.isEmpty()) {
                outputMovie.addTrack(new AppendTrack(listTracks.toArray(new Track[listTracks.size()])));
            }
            Container container = new DefaultMp4Builder().build(outputMovie);
            FileChannel fileChannel = new RandomAccessFile(String.format(targetFile), "rw").getChannel();
            container.writeContainer(fileChannel);
            fileChannel.close();
            return true;
        }
        catch (IOException e) {
            Log.e(TAG, "Error merging media files. exception: "+e.getMessage());
            return false;
        }
    }

    private void tick() {
        mStartTime = mStartTime + 100;
//        Log.e(TAG, "StartTime: "+mStartTime+" | "+"minutes: "+((mStartTime  / 60000))+ " | Seconds: "+((mStartTime  / 1000) % 60));
        int minutes = (int) (mStartTime  / 60000);
        int seconds = (int) (mStartTime  / 1000) % 60;
        String duration = minutes+":"+(seconds < 10 ? "0"+seconds : seconds);
//        Log.e(TAG, duration);
        if (mRecorder != null) {
            amplitudes[i] = mRecorder.getMaxAmplitude();
//            Log.d("Voice Recorder","amplitude: "+(amplitudes[i] * 100 / 32767));
            if (i >= amplitudes.length -1) {
                i = 0;
            } else {
                ++i;
            }
        }
        if(mTickListener != null){
            mTickListener.onTick(duration);
        }
    }

    public interface RecorderTickListener{
        void onTick(String duration);
    }
}
