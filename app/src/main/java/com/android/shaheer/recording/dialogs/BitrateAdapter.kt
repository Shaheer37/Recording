package com.android.shaheer.recording.dialogs

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.android.shaheer.recording.R
import com.android.shaheer.recording.utils.Constants

class BitrateAdapter(
        private val bitrates: MutableList<BitrateItem>,
        private var selectedItemIndex: Int,
        private val listener: OnBitrateSelectedListener
): RecyclerView.Adapter<BitrateAdapter.ViewHolder>() {

    companion object{
        fun createBitrates(selectedBitrate: Int): Pair<MutableList<BitrateItem>, Int>{
            var selectedItemIndex = 0
            val bitrates: MutableList<BitrateItem> = mutableListOf()
            Constants.Audio.BITRATES.forEachIndexed {index, bitrate ->
                if(selectedBitrate == bitrate) selectedItemIndex = index
                bitrates += BitrateItem(
                        bitrate,
                        Constants.Audio.SIZES[index],
                        selectedBitrate == bitrate
                )
            }
            return Pair(bitrates,selectedItemIndex)
        }
    }

    override fun getItemCount(): Int = bitrates.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.row_bitrate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindView()
    }

    fun onBitrateSelected(selectedBitrate: Int){
        if(selectedBitrate != bitrates[selectedItemIndex].bitrate) {
            bitrates[selectedItemIndex].isSelected = false
            notifyItemChanged(selectedItemIndex)
            bitrates.forEachIndexed { index, item ->
                if (item.isSelected && item.bitrate != selectedBitrate) {
                    item.isSelected = false
                }

                if (item.bitrate == selectedBitrate) {
                    item.isSelected = true
                    selectedItemIndex = index
                }
            }
            notifyItemChanged(selectedItemIndex)
            listener.onBitrateSelected(selectedBitrate)
        }
    }

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view){
        @BindView(R.id.tv_bitrate) lateinit var tvBitrate: TextView
        @BindView(R.id.tv_size) lateinit var tvSize: TextView

        init { ButterKnife.bind(this, view) }

        fun bindView(){
            val item = bitrates[adapterPosition]

            tvBitrate.text = "${item.bitrate/1000} Kbps"
            tvSize.text = "${item.size} MB/Minute"

            val textColor = if(item.isSelected) R.color.lightBlue else android.R.color.white
            tvBitrate.setTextColor(ResourcesCompat.getColor(
                    view.resources, textColor,null
            ))

            tvSize.setTextColor(ResourcesCompat.getColor(
                    view.resources, textColor,null
            ))

            view.setOnClickListener { onBitrateSelected(item.bitrate) }
        }
    }

    interface OnBitrateSelectedListener {
        fun onBitrateSelected(bitrate: Int)
    }
}