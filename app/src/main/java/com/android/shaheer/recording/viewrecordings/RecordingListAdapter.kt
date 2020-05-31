package com.android.shaheer.recording.viewrecordings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.shaheer.recording.R
import com.android.shaheer.recording.databinding.RowRecordBinding
import com.android.shaheer.recording.model.RecordItem

class RecordingListAdapter(
    private val itemInteractionListener: ItemInteractionListener
): ListAdapter<RecordItem,RecordingListAdapter.ViewHolder>(ItemDiffCallback()) {

    companion object{
        private const val TAG = "RecordingListAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowRecordBinding.inflate(
                LayoutInflater.from(parent.context)
                , parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView()
    }

    inner class ViewHolder(val binding: RowRecordBinding): RecyclerView.ViewHolder(binding.root){

        init {
            binding.root.setOnClickListener { itemInteractionListener.onItemClicked(adapterPosition) }
            binding.root.setOnLongClickListener {
                itemInteractionListener.onItemSelected(adapterPosition)
                true
            }
        }

        fun bindView(){
            binding.item = getItem(adapterPosition)
            binding.executePendingBindings()
        }
    }

    interface ItemInteractionListener{
        fun onItemSelected(position: Int)
        fun onItemClicked(position: Int)
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