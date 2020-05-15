package com.android.shaheer.recording.utils;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;

import java.io.File;
import java.io.IOException;

public class Player {
    public static final String TAG = Player.class.getSimpleName();

    private static final short DELAY_MILLI = 100;

    public enum PlayingStatus {
        notPlaying, playing, paused
    }

    private PlayingStatus mPlayingStatus = PlayingStatus.notPlaying;
    public PlayingStatus getPlayingStatus() {return mPlayingStatus;}

    private MediaPlayer mMediaPlayer;
    private PlayerEventListener mEventListener;

    private int trackPlayedLength = 0;
    public int getTrackPlayedLength() { return trackPlayedLength; }

    private int trackDuration = 0;
    public int getTrackDuration() { return trackDuration; }

    private boolean isPrepared = false;

    private Handler mHandler = new Handler();
    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            if(mPlayingStatus == PlayingStatus.playing && mMediaPlayer.isPlaying()){
                trackPlayedLength = mMediaPlayer.getCurrentPosition();
                double duration = mMediaPlayer.getDuration();
                mEventListener.onDurationUpdate((double)trackPlayedLength, duration);
            }
            mHandler.postDelayed(mTickExecutor,DELAY_MILLI);
        }
    };

    public boolean isPlaying(){
        return (mPlayingStatus == PlayingStatus.playing);
    }

    public boolean isPaused(){
        return (mPlayingStatus == PlayingStatus.paused);
    }

    public boolean isNotPlaying(){
        return (mPlayingStatus == PlayingStatus.notPlaying);
    }

    public Player(PlayerEventListener playerEventListener) {
        mEventListener = playerEventListener;
    }

    public boolean play(String track){
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stop();
                    if(mEventListener != null){
                        mEventListener.onTrackCompleted();
                    }
                }
            });
            Uri path = Uri.fromFile(new File(track));
            mMediaPlayer.setDataSource(String.valueOf(path));
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(player -> {
                start();
                isPrepared = true;
            });
        }catch (IOException exception){
            isPrepared = false;
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    private void start(){
        mMediaPlayer.start();
        mPlayingStatus = PlayingStatus.playing;
        mHandler.postDelayed(mTickExecutor, DELAY_MILLI);
        if(mEventListener != null) mEventListener.onTrackStarted(mMediaPlayer.getDuration());
        trackDuration = mMediaPlayer.getDuration();
    }

    public boolean pause(){
        if(mPlayingStatus == PlayingStatus.playing && mMediaPlayer.isPlaying()){
            mPlayingStatus = PlayingStatus.paused;
            mMediaPlayer.pause();
            trackPlayedLength = mMediaPlayer.getCurrentPosition();
            mHandler.removeCallbacks(mTickExecutor);
            return true;
        }else{
            return false;
        }
    }

    public boolean resume(){
        try{
            if(mPlayingStatus != PlayingStatus.playing
                    && isPrepared
                    && !mMediaPlayer.isPlaying()
            ){
                mMediaPlayer.seekTo(trackPlayedLength);
                start();
                return true;
            }else return false;
        }catch (IllegalStateException e){
            e.printStackTrace();
            return false;
        }
    }

    public void stop(){
        if(mMediaPlayer != null){
            mPlayingStatus = PlayingStatus.notPlaying;
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
        mHandler.removeCallbacks(mTickExecutor);
    }

    public void seek(double position){
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            int seekPosition = (int)((position/100)*mMediaPlayer.getDuration());
            mMediaPlayer.seekTo(seekPosition);
        }
        else if(mMediaPlayer!= null && isPaused()){
            trackPlayedLength = (int)((position/100)*mMediaPlayer.getDuration());
            resume();
        }
    }


    public interface PlayerEventListener {
        void onTrackCompleted();
        void onTrackStarted(long duration);
        void onDurationUpdate(Double position, Double duration);
    }
}
