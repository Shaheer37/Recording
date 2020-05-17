package com.android.shaheer.recording.viewrecordings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem

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

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view){
        @BindView(R.id.tv_recording_title) lateinit var tvRecordingTitle: TextView
        @BindView(R.id.tv_recording_duration) lateinit var tvRecordingDuration: TextView
        @BindView(R.id.btn_play_pause) lateinit var btnPlayPause: ImageButton
        @BindView(R.id.cv_item) lateinit var cvItem: CardView
        @BindView(R.id.cl_item) lateinit var clItem: ConstraintLayout

        init { ButterKnife.bind(this, view) }

        fun bindView(){
            val item = getItem(adapterPosition)
            tvRecordingTitle.text = item.recordAddress
            tvRecordingDuration.text = item.recordDuration

            if(item.isSelected){
                cvItem.cardElevation = cvItem.resources.getDimensionPixelSize(R.dimen.record_cv_selected_elevation).toFloat()
                clItem.background = clItem.resources.getDrawable(R.drawable.bg_row_record_selected, null)
            }else{
                cvItem.cardElevation = cvItem.resources.getDimensionPixelSize(R.dimen.record_cv_elevation).toFloat()
                clItem.background = clItem.resources.getDrawable(R.drawable.bg_row_record, null)
            }

            cvItem.setOnLongClickListener {
                itemInteractionListener.onItemSelected(adapterPosition)
                true
            }
            cvItem.setOnClickListener { itemInteractionListener.onItemClicked(adapterPosition) }
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
        return oldItem.isSelected == newItem.isSelected
    }
}