package com.android.shaheer.recording.dialogs.configs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.android.shaheer.recording.R
import com.android.shaheer.recording.utils.Constants.Audio.CHANNELS_MONO
import com.android.shaheer.recording.utils.Constants.Audio.CHANNELS_STEREO
import com.android.shaheer.recording.utils.SessionManager
import com.android.shaheer.recording.utils.SpacingItemDecoration
import kotlinx.android.synthetic.main.dialog_configs.*

class ConfigsDialog (
        context: Context,
        private val listener: OnCloseConfigsDialogListener
) : Dialog(context), BitrateAdapter.OnBitrateSelectedListener {

    private val sessionManager = SessionManager(context)

    override fun onBackPressed() {
        super.onBackPressed()
        listener.onCloseConfigsDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = View.inflate(context, R.layout.dialog_configs, null)
        setContentView(view)

        btn_channel_mono.setOnClickListener {
            sessionManager.channels = CHANNELS_MONO
            setupChannelBtns(CHANNELS_MONO)
        }

        btn_channel_stereo.setOnClickListener {
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
        rv_bitrates.adapter = BitrateAdapter(bitrates.first, bitrates.second, this)
        rv_bitrates.addItemDecoration(
                SpacingItemDecoration(
                        context.resources.getDimension(R.dimen.record_row_vertical_spacing).toInt()
                )
        )
        setupChannelBtns(sessionManager.channels)
    }

    private fun setupChannelBtns(channels: Int){
        when(channels){
            CHANNELS_MONO -> {
                btn_channel_mono.setChannelSelected(true)
                btn_channel_stereo.setChannelSelected(false)
            }
            CHANNELS_STEREO-> {
                btn_channel_mono.setChannelSelected(false)
                btn_channel_stereo.setChannelSelected(true)
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