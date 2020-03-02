package com.android.shaheer.recording.editrecordings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem

class RecordingListAdapter(): ListAdapter<RecordItem,RecordingListAdapter.ViewHolder>(ItemDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView(getItem(position))
    }

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view){
        @BindView(R.id.tv_recording_title) lateinit var tvRecordingTitle: TextView
        @BindView(R.id.tv_recording_duration) lateinit var tvRecordingDuration: TextView
        init { ButterKnife.bind(this, view) }

        fun bindView(item: RecordItem){
            tvRecordingTitle.text = item.recordAddress
            tvRecordingDuration.text = item.recordDuration
        }
    }
}

class ItemDiffCallback : DiffUtil.ItemCallback<RecordItem>() {
    override fun areItemsTheSame(oldItem: RecordItem, newItem: RecordItem): Boolean {
        return oldItem.recordAddress == newItem.recordAddress &&
                oldItem.recordDuration == oldItem.recordDuration
    }

    override fun areContentsTheSame(oldItem: RecordItem, newItem: RecordItem): Boolean {
        return oldItem.isPlaying == newItem.isPlaying
    }
}