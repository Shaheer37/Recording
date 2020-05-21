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
import com.android.shaheer.recording.R
import com.android.shaheer.recording.model.RecordItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.row_record.*

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

    inner class ViewHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer{

        fun bindView(){
            val item = getItem(adapterPosition)
            tv_recording_title.text = item.recordAddress
            tv_recording_duration.text = item.recordDuration

            if(item.isSelected){
                cv_item.cardElevation = containerView.resources.getDimensionPixelSize(R.dimen.record_cv_selected_elevation).toFloat()
                cl_item.background = containerView.resources.getDrawable(R.drawable.bg_row_record_selected, null)
            }else{
                cv_item.cardElevation = containerView.resources.getDimensionPixelSize(R.dimen.record_cv_elevation).toFloat()
                cl_item.background = containerView.resources.getDrawable(R.drawable.bg_row_record, null)
            }

            cv_item.setOnLongClickListener {
                itemInteractionListener.onItemSelected(adapterPosition)
                true
            }
            cv_item.setOnClickListener { itemInteractionListener.onItemClicked(adapterPosition) }
            btn_play_pause.setOnClickListener { itemInteractionListener.onItemPlayClicked(adapterPosition) }
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