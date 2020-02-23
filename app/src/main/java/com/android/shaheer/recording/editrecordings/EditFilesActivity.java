package com.android.shaheer.recording.editrecordings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.android.shaheer.recording.R;
import com.android.shaheer.recording.model.RecordItem;
import com.android.shaheer.recording.utils.SessionManager;
import com.android.shaheer.recording.utils.FilesUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;


public class EditFilesActivity extends AppCompatActivity implements EditFilesAdapter.EditListInterface{
    EditFilesAdapter adapter;

    private ArrayList<RecordItem> recordList;
    private ListView listview;

    private SessionManager mSessionManager ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recording_list_edit);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mSessionManager = new SessionManager(this);

        recordList = new ArrayList<RecordItem>();
        File directory = new File(FilesUtil.getDir(this));
        final File list[] = directory.listFiles();
        for (int i = 0; i < list.length; i++) {
            String name = list[i].getName();
            if (name.contains("m4a")) {
                String audioFilename = name.split("\\.m4a")[0];
                MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
                metaRetriever.setDataSource(directory.getAbsolutePath() + "/" + name);

                // convert duration to minute:seconds
                String duration =
                        metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                Log.v("time", duration);
                long dur = Long.parseLong(duration);
                String totalTime = String.format("%02d:%02d:%02d",
                        TimeUnit.MILLISECONDS.toHours(dur),
                        TimeUnit.MILLISECONDS.toMinutes(dur),
                        TimeUnit.MILLISECONDS.toSeconds(dur) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(dur))
                );
                recordList.add(new RecordItem(audioFilename, totalTime));
                metaRetriever.release();
            }

        }

        Collections.reverse(recordList);

        listview = (ListView) findViewById(R.id.audiolist);
        adapter = new EditFilesAdapter(this, recordList, this);

        listview.setAdapter(adapter);
        listview.setTextFilterEnabled(true);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long arg3) {
                showEditRecordDialog(position);
            }
        });
    }

    private void deleteAudioFile(String name) {
        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"+getString(R.string.app_name));
        String fileName = directory.getAbsolutePath() + "/" + name + ".m4a";

        if(mSessionManager.getLastRecording() != null && fileName.equalsIgnoreCase(mSessionManager.getLastRecording())){
            mSessionManager.setLastRecording(null);
        }
        new File(fileName).delete();
    }

    private void showEditRecordDialog(final int itemPosition){
        AlertDialog.Builder fileDialog = new AlertDialog.Builder(EditFilesActivity.this);

        TextView title = new TextView(EditFilesActivity.this);
        title.setText("Rename file");
        title.setTypeface(null, Typeface.BOLD);
        title.setTextSize(20);
        title.setPadding(10, 10, 10, 10);
        title.setGravity(Gravity.CENTER);
        fileDialog.setCustomTitle(title);

        LinearLayout layout = new LinearLayout(EditFilesActivity.this);
        LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(parms);
        layout.setGravity(Gravity.CLIP_VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Set an EditText view to get user input
        TextView Msg = new TextView(EditFilesActivity.this);
        Msg.setText("Please type the new name of file ");
        Msg.setPadding(10, 20, 0, 20);
        fileDialog.setView(Msg);

        final EditText input = new EditText(EditFilesActivity.this);
        input.setText(recordList.get(itemPosition).getRecordAddress());
        layout.addView(Msg);
        layout.addView(input);
        fileDialog.setView(layout);
        fileDialog.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String fileName = input.getText().toString() + ".m4a";
                        File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                                + "/"+getString(R.string.app_name));
                        File audioFile = new File(directory.getAbsolutePath() + "/" + recordList.get(itemPosition).getRecordAddress() + ".m4a");
                        audioFile.renameTo(new File(directory.getAbsolutePath() + "/" + fileName));
                        Intent EditScreen = new Intent(getApplicationContext(),
                                EditFilesActivity.class);
                        startActivity(EditScreen);
                        Toast.makeText(EditFilesActivity.this, "file rename successfully", Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

        fileDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                        dialog.dismiss();
                    }
                });
        fileDialog.create();
        fileDialog.show();
    }

    @Override
    public void deleteRecord(int itemPosition) {
        deleteAudioFile(recordList.get(itemPosition).getRecordAddress());
        recordList.remove(itemPosition);
        Collections.reverse(recordList);
        adapter.setRecordList(recordList);
    }

    @Override
    public void showDeleteButton(int itemPosition, boolean showButton) {
        View rowView = listview.getChildAt(itemPosition -
                listview.getFirstVisiblePosition());

        if(rowView == null)
            return;
        adapter.toggleDeleteButton(rowView, showButton);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_archive, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        if (id == R.id.action_done) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
