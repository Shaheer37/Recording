package com.android.shaheer.recording.viewrecordings;

import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.android.shaheer.recording.R;
import com.android.shaheer.recording.editrecordings.EditFilesActivity;
import com.android.shaheer.recording.model.RecordItem;
import com.android.shaheer.recording.utils.SessionManager;
import com.android.shaheer.recording.utils.FilesUtil;
import com.android.shaheer.recording.utils.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class ListFilesActivity extends AppCompatActivity implements
        ListFilesAdapter.ListInterface,
        Player.PlayerEventListener
{
    public static final String TAG = ListFilesActivity.class.getSimpleName();
    private static final int EDIT_SCREEN_IDENTIFIER = 101;

    private ListView listview;
    private ListFilesAdapter adapter;
    private Player mPlayer;
    private int playingFilePosition = -1;

    private ArrayList<RecordItem> recordList;
    private SessionManager mSessionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPlayer = new Player(this, this);
        mSessionManager = new SessionManager(this);

        recordList = new ArrayList();
        listview = (ListView) findViewById(R.id.audiolist);
        adapter = new ListFilesAdapter(this, recordList, this);

        listview.setAdapter(adapter);
        listview.setTextFilterEnabled(true);

        createList();
    }

    public void createList(){
        recordList = new ArrayList();
//        File directory = getFilesDir();
        File directory = new File(FilesUtil.getDir(this));
        Log.e("File Dir: ", directory.getPath());
        final File list[] = directory.listFiles();
        for (int i = 0; i < list.length; i++) {
            String name = list[i].getName();
            String recordAddress = null;
            if (name.contains("m4a")) {
                recordAddress = name.split("\\.m4a")[0];
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(directory.getPath() + "/" + name);

                // get mp3 info
                // convert duration to minute:seconds
                String duration =
                        metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                int dur = Integer.parseInt(duration);
                String recordDuration = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(dur),
                        TimeUnit.MILLISECONDS.toMinutes(dur) -
                                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(dur)), // The change is in this line
                        TimeUnit.MILLISECONDS.toSeconds(dur) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(dur)));
                recordList.add(new RecordItem(recordAddress, recordDuration));
                metaRetriever.release();
            }
        }
        Collections.reverse(recordList);

        adapter = new ListFilesAdapter(this, recordList, this);
        listview.setAdapter(adapter);
    }

    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == EDIT_SCREEN_IDENTIFIER){
            createList();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if((mPlayer.isPlaying() || mPlayer.isPaused()) && playingFilePosition != -1){
            updatePlayingStatus(playingFilePosition, false);
            stopPlayingFile();
        }
    }

    private void stopPlayingFile(){
        if(mPlayer != null && !mPlayer.isNotPlaying()) {
            mPlayer.stop();
        }
        playingFilePosition = -1;
    }

    private void updatePlayingStatus(int itemPosition, boolean isPlaying){
        if(isPlaying){
            playingFilePosition = itemPosition;
        }
        recordList.get(itemPosition).setPlaying(isPlaying);
        adapter.setRecordList(recordList);
        View rowView = getListRow(itemPosition);

        if(rowView == null) return;

        adapter.togglePlayingStatus(rowView, isPlaying);
    }

    private View getListRow(int itemPosition){
        return listview.getChildAt(itemPosition -
                listview.getFirstVisiblePosition());
    }

    @Override
    public void playRecord(String fileAddr,final int itemPosition) {

        File directory = new File(FilesUtil.getDir(this));

        if(playingFilePosition == itemPosition){
            Log.e(TAG, "1");
            if(mPlayer.isPaused()){
                mPlayer.resume();
                View rowView = getListRow(itemPosition);
                if(rowView == null) return;

                adapter.resumeRow(rowView);
            }else{
                mPlayer.pause();

                View rowView = getListRow(itemPosition);
                if(rowView == null) return;

                adapter.pauseRow(rowView);
            }
        }else if(playingFilePosition < 0){
            Log.e(TAG, "2");
            mPlayer.play(directory.getAbsolutePath() + "/" + fileAddr + ".m4a");
            updatePlayingStatus(itemPosition, true);
        }else{
            Log.e(TAG, "3");
            updatePlayingStatus(playingFilePosition, false);
            stopPlayingFile();
            mPlayer.play(directory.getAbsolutePath() + "/" + fileAddr + ".m4a");
            updatePlayingStatus(itemPosition, true);
        }
        Log.e(TAG, "Position: " +itemPosition + " | "+playingFilePosition);
    }

    @Override
    public void onTrackCompleted() {
        updatePlayingStatus(playingFilePosition, false);
        stopPlayingFile();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_audio_archive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (id == R.id.action_edit) {
            Intent EditScreen = new Intent(getApplicationContext(),
                    EditFilesActivity.class);
            startActivityForResult(EditScreen, EDIT_SCREEN_IDENTIFIER);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}