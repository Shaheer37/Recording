package com.android.shaheer.recording.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.android.shaheer.recording.R
import com.android.shaheer.recording.utils.Constants.Audio.CHANNELS_MONO
import com.android.shaheer.recording.utils.Constants.Audio.CHANNELS_STEREO
import com.android.shaheer.recording.utils.SessionManager

class ConfigsDialog (
        context: Context,
        private val listener: OnCloseConfigsDialogListener
) : Dialog(context), BitrateAdapter.OnBitrateSelectedListener {

    @BindView(R.id.rv_bitrates) lateinit var rvBitrates: RecyclerView
    @BindView(R.id.btn_channel_mono) lateinit var btnChannelMono: Button
    @BindView(R.id.btn_channel_stereo) lateinit var btnChannelStereo: Button

    private val sessionManager = SessionManager(context)

    override fun onBackPressed() {
        super.onBackPressed()
        listener.onCloseConfigsDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = View.inflate(context, R.layout.dialog_configs, null)
        ButterKnife.bind(this, view)
        setContentView(view)
    }

    override fun show() {
        super.show()
        setCanceledOnTouchOutside(false)
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
        window?.decorView?.background?.alpha = 0

        val bitrates = BitrateAdapter.createBitrates(sessionManager.bitrate)
        rvBitrates.adapter = BitrateAdapter(bitrates.first, bitrates.second, this)
        setupChannelBtns(sessionManager.channels)
    }

    private fun setupChannelBtns(channels: Int){
        when(channels){
            CHANNELS_MONO -> {
                btnChannelMono.setChannelSelected(true)
                btnChannelStereo.setChannelSelected(false)
            }
            CHANNELS_STEREO-> {
                btnChannelMono.setChannelSelected(false)
                btnChannelStereo.setChannelSelected(true)
            }
        }
    }

    @OnClick(R.id.btn_channel_mono)
    fun onMonoChannelSelected(){
        sessionManager.channels = CHANNELS_MONO
        setupChannelBtns(CHANNELS_MONO)
    }

    @OnClick(R.id.btn_channel_stereo)
    fun onStereoChannelSelected(){
        sessionManager.channels = CHANNELS_STEREO
        setupChannelBtns(CHANNELS_STEREO)
    }

    override fun onBitrateSelected(bitrate: Int) {
        sessionManager.bitrate = bitrate
    }

    private fun Button.setChannelSelected(isSelected: Boolean = false){
        val color = if(isSelected) R.color.lightBlue else android.R.color.white
        setTextColor(ResourcesCompat.getColor(context.resources,color, null))
    }

    interface OnCloseConfigsDialogListener{
        fun onCloseConfigsDialog()
    }
}