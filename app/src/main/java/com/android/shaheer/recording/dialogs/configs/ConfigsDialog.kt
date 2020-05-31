package com.android.shaheer.recording.dialogs.configs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.android.shaheer.recording.R
import com.android.shaheer.recording.databinding.DialogConfigsBinding
import com.android.shaheer.recording.utils.Constants.Audio.CHANNELS_MONO
import com.android.shaheer.recording.utils.Constants.Audio.CHANNELS_STEREO
import com.android.shaheer.recording.utils.SessionManager
import com.android.shaheer.recording.utils.SpacingItemDecoration

class ConfigsDialog (
        context: Context,
        private val listener: OnCloseConfigsDialogListener
) : Dialog(context), BitrateAdapter.OnBitrateSelectedListener {

    private val sessionManager = SessionManager(context)

    private val binding: DialogConfigsBinding = DialogConfigsBinding.inflate(LayoutInflater.from(context))

    override fun onBackPressed() {
        super.onBackPressed()
        listener.onCloseConfigsDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)

        binding.btnChannelMono.setOnClickListener {
            sessionManager.channels = CHANNELS_MONO
            setupChannelBtns(CHANNELS_MONO)
        }

        binding.btnChannelStereo.setOnClickListener {
            sessionManager.channels = CHANNELS_STEREO
            setupChannelBtns(CHANNELS_STEREO)
        }
    }

    override fun show() {
        super.show()
        setCanceledOnTouchOutside(false)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.decorView?.background?.alpha = 0

        val bitrates = BitrateAdapter.createBitrates(sessionManager.bitrate)
        binding.rvBitrates.adapter = BitrateAdapter(bitrates.first, bitrates.second, this)
        binding.rvBitrates.addItemDecoration(
                SpacingItemDecoration(
                        context.resources.getDimension(R.dimen.record_row_vertical_spacing).toInt()
                )
        )
        setupChannelBtns(sessionManager.channels)
    }

    private fun setupChannelBtns(channels: Int){
        when(channels){
            CHANNELS_MONO -> {
                binding.btnChannelMono.setChannelSelected(true)
                binding.btnChannelStereo.setChannelSelected(false)
            }
            CHANNELS_STEREO-> {
                binding.btnChannelMono.setChannelSelected(false)
                binding.btnChannelStereo.setChannelSelected(true)
            }
        }
    }

    override fun onBitrateSelected(bitrate: Int) {
        sessionManager.bitrate = bitrate
    }

    private fun Button.setChannelSelected(isSelected: Boolean = false){
        val color = if(isSelected) R.color.textcolor else R.color.notSelectedChannel
        setTextColor(ResourcesCompat.getColor(context.resources,color, null))
    }

    interface OnCloseConfigsDialogListener{
        fun onCloseConfigsDialog()
    }
}