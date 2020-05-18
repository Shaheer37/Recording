package com.android.shaheer.recording.dialogs.configs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.shaheer.recording.R
import com.android.shaheer.recording.utils.Constants
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.row_bitrate.*

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

    inner class ViewHolder(override val containerView: View): RecyclerView.ViewHolder(containerView), LayoutContainer{

        fun bindView(){
            val item = bitrates[adapterPosition]

            tv_bitrate.text = "${item.bitrate/1000} Kbps"
            tv_size.text = "${item.size} MB/Minute"

            if(item.isSelected){
                cv_item.cardElevation = containerView.resources.getDimensionPixelSize(R.dimen.bitrate_cv_selected_elevation).toFloat()
                cl_item.background = containerView.resources.getDrawable(R.drawable.bg_row_bitrate_selected, null)
            }else{
                cv_item.cardElevation = containerView.resources.getDimensionPixelSize(R.dimen.bitrate_cv_elevation).toFloat()
                cl_item.background = containerView.resources.getDrawable(R.drawable.bg_row_bitrate, null)
            }

            containerView.setOnClickListener { onBitrateSelected(item.bitrate) }
        }
    }

    interface OnBitrateSelectedListener {
        fun onBitrateSelected(bitrate: Int)
    }
}