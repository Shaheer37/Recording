package com.android.shaheer.recording.viewrecordings;


import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.shaheer.recording.R;
import com.android.shaheer.recording.model.RecordItem;

import java.util.ArrayList;

public class ListFilesAdapter extends ArrayAdapter<RecordItem> {
	public static final String COLOR_PLAYING = "#0080ff";
	public static final String COLOR_NOT_PLAYING = "#000000";
	private Context context;
	private ArrayList<RecordItem> recordList;
	ListInterface listInterface;

	public ListFilesAdapter(Context context, ArrayList<RecordItem> recordList, ListInterface listInterface) {
		super(context, R.layout.list_item, recordList);
		this.context = context;
		this.recordList = recordList;
		this.listInterface = listInterface;
	}

	public ArrayList<RecordItem> getRecordList() {
		return recordList;
	}

	public void setRecordList(ArrayList<RecordItem> recordList) {
		this.recordList = recordList;
	}

	@SuppressLint("ViewHolder")
	@Override
	public View getView(final int position, View view, final ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View rowView = inflater.inflate(R.layout.list_item, parent, false);

		LinearLayout llRecord = (LinearLayout) rowView.findViewById(R.id.ll_record);
		TextView txtTitle = (TextView) rowView.findViewById(R.id.tv_recording_title);
		TextView durationFile = (TextView) rowView.findViewById(R.id.tv_recording_duration);
		ImageButton btnPlayPause = (ImageButton) rowView.findViewById(R.id.btn_play_pause);

		durationFile.setText(recordList.get(position).getRecordDuration());

		if(recordList.get(position).isPlaying()){
			((RelativeLayout) rowView).setBackgroundColor(context.getResources().getColor(R.color.grey));
			btnPlayPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_pause_circle_green));
		}else{
			((RelativeLayout) rowView).setBackgroundColor(context.getResources().getColor(android.R.color.white));
			btnPlayPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play_circle_blue));
		}
		txtTitle.setText(recordList.get(position).getRecordAddress());

		btnPlayPause.setClickable(true);
		btnPlayPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				listInterface.playRecord(recordList.get(position).getRecordAddress(), position);
			}
		});

		llRecord.setClickable(true);
		llRecord.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				listInterface.playRecord(recordList.get(position).getRecordAddress(), position);
			}
		});

		return rowView;
	}

	public void togglePlayingStatus(View rowView, boolean isPlaying){
		ImageButton btnPlayPause = (ImageButton) rowView.findViewById(R.id.btn_play_pause);
		if(isPlaying){
			((RelativeLayout) rowView).setBackgroundColor(context.getResources().getColor(R.color.grey));
			btnPlayPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_pause_circle_green));
		}else{
			((RelativeLayout) rowView).setBackgroundColor(context.getResources().getColor(android.R.color.white));
			btnPlayPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play_circle_blue));
		}
	}

	public void pauseRow(View rowView){
		ImageButton btnPlayPause = (ImageButton) rowView.findViewById(R.id.btn_play_pause);
		btnPlayPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_play_circle_blue));
	}

	public void resumeRow(View rowView){
		ImageButton btnPlayPause = (ImageButton) rowView.findViewById(R.id.btn_play_pause);
		btnPlayPause.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_pause_circle_green));
	}

	public interface ListInterface{
		void playRecord(String fileAddr, int itemPosition);
	}

}
