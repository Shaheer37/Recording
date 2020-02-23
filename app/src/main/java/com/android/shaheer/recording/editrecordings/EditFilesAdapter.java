package com.android.shaheer.recording.editrecordings;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import com.android.shaheer.recording.R;
import com.android.shaheer.recording.model.RecordItem;

import java.util.ArrayList;

public class EditFilesAdapter extends ArrayAdapter<RecordItem> {
	private final Context context;
	private ArrayList<RecordItem> recordList;
	private EditListInterface editListInterface;


	public EditFilesAdapter(Activity context, ArrayList<RecordItem> recordList, EditListInterface editListInterface) {
		super(context, R.layout.list_single_edit, recordList);
		this.context = context;
		this.recordList = recordList;
		this.editListInterface = editListInterface;
	}

	public ArrayList<RecordItem> getRecordList() {
		return recordList;
	}

	public void setRecordList(ArrayList<RecordItem> recordList) {
		this.recordList = recordList;
		notifyDataSetChanged();
	}

	@SuppressLint("ViewHolder")
	@Override
	public View getView(final int position, View view, final ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View rowView = inflater.inflate(R.layout.list_single_edit, parent, false);
		TextView txtTitle = (TextView) rowView.findViewById(R.id.txt);
		txtTitle.setText(recordList.get(position).getRecordAddress());
		TextView durationFile = (TextView) rowView.findViewById(R.id.duration);
		durationFile.setText(recordList.get(position).getRecordDuration());

		final Button del = (Button) rowView.findViewById(R.id.del);

		ImageView delete = (ImageView) rowView.findViewById(R.id.delete);
		delete.setTag(position);

		delete.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.e("delete", "onClick()");
				editListInterface.showDeleteButton(position, del.getVisibility() == View.GONE);
			}
		});

		del.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Log.e("del", "onClick()");
				editListInterface.deleteRecord(position);
			}

		});

		return rowView;
	}

	public void toggleDeleteButton(View rowView, boolean showButton) {
		Button btnDelRecord = (Button) rowView.findViewById(R.id.del);
		if(showButton) {
			btnDelRecord.setVisibility(View.VISIBLE);
		}else{
			btnDelRecord.setVisibility(View.GONE);
		}
	}

	public interface EditListInterface{
		void deleteRecord(int itemPosition);
		void showDeleteButton(int itemPosition, boolean showButton);
	}

}
