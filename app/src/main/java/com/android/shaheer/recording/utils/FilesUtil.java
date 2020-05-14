package com.android.shaheer.recording.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.util.Log;

import com.android.shaheer.recording.R;
import com.android.shaheer.recording.model.RecordItem;

import org.mp4parser.Container;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AppendTrack;

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
import java.util.concurrent.TimeUnit;

public class FilesUtil {
    private static final String TAG = "FilesUtil";

    public static final String RECORDING_FILE_REGEX = "^[\\w,:-]+\\.(m4a|wav)$";

    public static final String DATE_FORMAT = "dd-MM-yyyy,HH:mm:ss";
    private static final String MERGED_FILE = "merged";

    public static String getDir(Context context){
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/"+context.getString(R.string.app_name);
    }

    private ArrayList<File> recordPieces;
    public void addRecordingPiece(File piece) { recordPieces.add(piece); }

    FilesUtil(){
        recordPieces = new ArrayList<>();
    }

    private String getRecordName(String fileExtension){
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        return "RECORDING_"+ dateFormat.format(new Date()) + "." + fileExtension;
    }

    public File getFile(Context context, String fileName, String fileExtension) {
        if(fileName == null) fileName = getRecordName(fileExtension);
        File file = new File(getDir(context)+"/"+fileName);
        Log.e(TAG, file.getAbsolutePath());
        return file;
    }

    public boolean mergeFiles(Context context, File destination, String fileExtension){
        if(recordPieces.size()>1){
            Log.e(TAG, "more files");
            File mergedFile = getFile(context, MERGED_FILE, fileExtension);
            if(mergeMediaFiles(recordPieces.toArray(new File[recordPieces.size()]), mergedFile.getAbsolutePath())){
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

    private boolean mergeMediaFiles(File sourceFiles[], String targetFile) {
        try {
            List<Movie> listMovies = new ArrayList<>();
            for (File filename : sourceFiles) {
                listMovies.add(MovieCreator.build(filename.getAbsolutePath()));
            }
            List<Track> listTracks = new LinkedList<>();
            for (Movie movie : listMovies) {
                for (Track track : movie.getTracks()) {
                    listTracks.add(track);
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

    public static RecordItem createRecordItem(File recording, MediaMetadataRetriever metaRetriever){
        String fileName = recording.getName();
        if (fileName.matches(RECORDING_FILE_REGEX)) {
            String[] split = fileName.split("\\.");
            String audioFileName = split[0];
            String audioFileExt = split[1];
            metaRetriever.setDataSource(recording.getAbsolutePath());

            // convert duration to minute:seconds
            String durationString = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long duration = Long.parseLong(durationString);
            String totalTime = formatDuration(duration);
            return new RecordItem(audioFileName, audioFileExt, totalTime, false);
        }else return null;
    }

    public static String formatDuration(Long duration){
        long hours = TimeUnit.MILLISECONDS.toHours(duration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration);
        if(hours>0) return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        else return String.format("%02d:%02d", minutes, seconds);
    }
}
