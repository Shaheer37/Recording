package com.android.shaheer.recording.utils;

import android.content.Context;
import android.os.Environment;
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

public class FilesUtil {
    private static final String TAG = "FilesUtil";

    public static final String DATE_FORMAT = "dd-MM-yyyy,HH:mm:ss";
    private static final String MERGED_FILE = "merged.m4a";

    public static String getDir(Context context){
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"+context.getString(R.string.app_name);
    }

    private ArrayList<File> recordPieces;
    public void addRecordingPiece(File piece) { recordPieces.add(piece); }

    FilesUtil(){
        recordPieces = new ArrayList<>();
    }

    private String getRecordName(){
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return "RECORDING_"+ dateFormat.format(new Date()) + ".m4a";
    }

    public File getFile(Context context, String fileName) {
        if(fileName == null) fileName = getRecordName();
        File file = new File(getDir(context)+"/"+fileName);
        Log.e(TAG, file.getAbsolutePath());
        return file;
    }

    public boolean mergeFiles(Context context, File destination){
        if(recordPieces.size()>1){
            Log.e(TAG, "more files");
            File mergedFile = getFile(context, MERGED_FILE);
            if(mergeMediaFiles(true, recordPieces.toArray(new File[recordPieces.size()]), mergedFile.getAbsolutePath())){
                for(File record: recordPieces){
                    record.delete();
                }
                recordPieces = new ArrayList<>();
                mergedFile.renameTo(destination);
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
}
