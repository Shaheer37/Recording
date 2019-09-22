package com.android.shaheer.recording.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

public class Player {
    public static final String TAG = Player.class.getSimpleName();
    enum PlayingStatus {
        notPlaying, playing, paused, ended
    }

    private Context mContext;

    private PlayingStatus mPlayingStatus = PlayingStatus.notPlaying;
    public PlayingStatus getmRecordingStatus() {return mPlayingStatus;}

    private MediaPlayer mMediaPlayer;
    private PlayerEventListener mEventListener;

    private int trackPlayedLength = 0;


    public boolean isPlaying(){
        return (mPlayingStatus == PlayingStatus.playing);
    }

    public boolean isPaused(){
        return (mPlayingStatus == PlayingStatus.paused);
    }

    public boolean isNotPlaying(){
        return (mPlayingStatus == PlayingStatus.notPlaying);
    }

    public Player(Context mContext, PlayerEventListener playerEventListener) {
        this.mContext = mContext;
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
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer arg0) {
                    mMediaPlayer.start();
                    mPlayingStatus = PlayingStatus.playing;
                }
            });
        }catch (IOException exception){
            exception.printStackTrace();
            return false;
        }
        return true;
    }

    public void pause(){
        if(mPlayingStatus == PlayingStatus.playing && mMediaPlayer.isPlaying()){
            mPlayingStatus = PlayingStatus.paused;
            mMediaPlayer.pause();
            trackPlayedLength = mMediaPlayer.getCurrentPosition();
        }
    }

    public void resume(){
        if(mPlayingStatus == PlayingStatus.paused && !mMediaPlayer.isPlaying()){
            mPlayingStatus = PlayingStatus.playing;
            mMediaPlayer.seekTo(trackPlayedLength);
            mMediaPlayer.start();
        }

    }

    public void stop(){
        if(mMediaPlayer != null){
            mPlayingStatus = PlayingStatus.ended;
            try {
                mMediaPlayer.stop();
                mMediaPlayer.release();
                mMediaPlayer = null;
            }catch (IllegalStateException e){
                e.printStackTrace();
            }
        }
    }


    public interface PlayerEventListener{
        void onTrackCompleted();
    }
}
