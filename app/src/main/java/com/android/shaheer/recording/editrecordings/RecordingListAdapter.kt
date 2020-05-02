package com.android.shaheer.recording.editrecordings

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem
import com.android.shaheer.recording.utils.Player

class RecordingListAdapter(
    private val itemInteractionListener: ItemInteractionListener
): ListAdapter<RecordItem,RecordingListAdapter.ViewHolder>(ItemDiffCallback()) {

    companion object{
        private const val TAG = "RecordingListAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.row_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView()
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        @BindView(R.id.tv_recording_title) lateinit var tvRecordingTitle: TextView
        @BindView(R.id.tv_recording_duration) lateinit var tvRecordingDuration: TextView
        @BindView(R.id.cv_record) lateinit var cvRecord: CardView
        @BindView(R.id.btn_play_pause) lateinit var btnPlayPause: ImageButton

        init { ButterKnife.bind(this, view) }

        fun bindView(){
            val item = getItem(adapterPosition)
            tvRecordingTitle.text = item.recordAddress
            tvRecordingDuration.text = item.recordDuration

            if(item.isSelected){
                cvRecord.cardElevation = cvRecord.resources.getDimensionPixelSize(R.dimen.record_cv_selected_elevation).toFloat()
            }else{
                cvRecord.cardElevation = cvRecord.resources.getDimensionPixelSize(R.dimen.record_cv_elevation).toFloat()
            }

            when(item.playingStatus){
                Player.PlayingStatus.playing -> {
                    Log.d(TAG,"Player.PlayingStatus.playing")
                    btnPlayPause.setImageDrawable(btnPlayPause.context.getDrawable(R.drawable.ic_pause_circle_green))
                }
                Player.PlayingStatus.paused -> {
                    btnPlayPause.setImageDrawable(btnPlayPause.context.getDrawable(R.drawable.ic_play_circle))
                }
                Player.PlayingStatus.notPlaying -> {
                    btnPlayPause.setImageDrawable(btnPlayPause.context.getDrawable(R.drawable.ic_play_circle))
                }
            }

            cvRecord.setOnLongClickListener {
                itemInteractionListener.onItemSelected(adapterPosition)
                true
            }
            cvRecord.setOnClickListener { itemInteractionListener.onItemClicked(adapterPosition) }
            btnPlayPause.setOnClickListener { itemInteractionListener.onItemPlayClicked(adapterPosition) }
        }


    }

    interface ItemInteractionListener{
        fun onItemSelected(position: Int)
        fun onItemClicked(position: Int)
        fun onItemPlayClicked(position: Int)
    }
}

class ItemDiffCallback : DiffUtil.ItemCallback<RecordItem>() {
    override fun areItemsTheSame(oldItem: RecordItem, newItem: RecordItem): Boolean {
        return oldItem.recordAddress == newItem.recordAddress &&
                oldItem.recordDuration == oldItem.recordDuration
    }

    override fun areContentsTheSame(oldItem: RecordItem, newItem: RecordItem): Boolean {
        return oldItem.playingStatus == newItem.playingStatus &&
                oldItem.isSelected == newItem.isSelected &&
                oldItem.playingStatus == newItem.playingStatus
    }
}