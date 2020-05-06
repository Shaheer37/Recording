package com.android.shaheer.recording.record


import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.android.shaheer.recording.MainViewModel
import com.android.shaheer.recording.MainViewModelFactory

import com.android.shaheer.recording.R
import com.android.shaheer.recording.utils.*
import com.omega_r.libs.OmegaCenterIconButton


class RecordingFragment : Fragment() {

    enum class RecordingStatus {
        notRecording, recording
    }

    @BindView(R.id.view_sine_wave) lateinit var sineWaveView: DynamicSineWaveView
    @BindView(R.id.tv_status) lateinit var tvRecording: TextView

    @BindView(R.id.group_recording) lateinit var groupRecording: Group

    @BindView(R.id.btn_start_recording) lateinit  var btnStartRecording: OmegaCenterIconButton
    @BindView(R.id.btn_play_last_recording) lateinit var btnPlayLastRecording: OmegaCenterIconButton

    @BindView(R.id.tv_record_duration) lateinit var tvRecordDuration: TextView
    @BindView(R.id.btn_recording_action) lateinit var btnRecordAction: ImageButton
    @BindView(R.id.btn_recording_stop) lateinit var btnRecordStop: ImageButton
    @BindView(R.id.tv_recording_action) lateinit var tvRecordAction: TextView

    @BindView(R.id.btn_audio_archive) lateinit var btnAudioArchive: Button

    private lateinit var recordingViewModel: RecordingViewModel
    private lateinit var mainViewModel: MainViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_recording, container, false)
        ButterKnife.bind(this, view)

        val stroke = CommonMethods.dipToPixels(context, 2f)
        sineWaveView.addWave(0.5f, 0.5f, 0f, 0, 0f) // Fist wave is for the shape of other waves.
        sineWaveView.addWave(0.5f, 6f, 1f, getColor(resources, R.color.lightBlue, requireActivity().theme), stroke)

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        activity?.let {
            mainViewModel = ViewModelProvider(
                    it.viewModelStore,
                    MainViewModelFactory()
            ).get(MainViewModel::class.java)
        }

        recordingViewModel = ViewModelProvider(
                viewModelStore,
                RecordingViewModelFactory(SessionManager(context.applicationContext)
            )
        ).get(RecordingViewModel::class.java)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recordingViewModel.bindService.observe(viewLifecycleOwner, EventObserver {
            val intent = Intent(context, RecordingService::class.java)
            context?.bindService(intent, it, Context.BIND_AUTO_CREATE)
        })

        recordingViewModel.unbindService.observe(viewLifecycleOwner, EventObserver {
            context?.unbindService(it)
        })

        recordingViewModel.isServiceBound.observe(viewLifecycleOwner, EventObserver{/*TODO: do something*/})

        recordingViewModel.state.observe(viewLifecycleOwner, Observer{
            when(it){
                RecordingStatus.recording -> setRecordingLayout()
                RecordingStatus.notRecording -> setInitialLayout()
            }
        })

        recordingViewModel.recordingState.observe(viewLifecycleOwner, EventObserver{setRecordingState(it)})

        recordingViewModel.showLastRecordingButton.observe(viewLifecycleOwner, EventObserver{
            if(it) btnPlayLastRecording.visibility = View.VISIBLE
            else btnPlayLastRecording.visibility = View.INVISIBLE
        })

        recordingViewModel.duration.observe(viewLifecycleOwner, Observer { tvRecordDuration.text = it  })
        recordingViewModel.amplitude.observe(viewLifecycleOwner, Observer { sineWaveView.baseWaveAmplitudeScale = it  })

        mainViewModel.hasAllPermissions.observe(viewLifecycleOwner, EventObserver{
            if(it){
                val intent = Intent(context, RecordingService::class.java)
                intent.action = RecordingService.ACTION_START
                context?.startService(intent)

                recordingViewModel.bindService()
            }
        })

        mainViewModel.hasStoragePermission.observe(viewLifecycleOwner, EventObserver{
            if(it){
//                val listFiles = Intent(context, ListFilesActivity::class.java)
//                startActivity(listFiles)
                findNavController().navigate(
                        RecordingFragmentDirections.actionRecordingToViewRecords()
                )
            }
        })

    }

    override fun onPause() {
        super.onPause()

        recordingViewModel.setStateInitial()

        if (CommonMethods.isServiceRunning(RecordingService::class.java, context)) {
            recordingViewModel.unbindService()
        }
    }

    override fun onResume() {
        super.onResume()
        recordingViewModel.setStateInitial()

        if (CommonMethods.isServiceRunning(RecordingService::class.java, context)) {
            recordingViewModel.bindService()
        }
    }

    @OnClick(R.id.btn_start_recording)
    fun onRecordAction() = mainViewModel.checkPermissions()

    @OnClick(R.id.btn_recording_action)
    fun onRecordingAction() = recordingViewModel.onRecordingAction()

    @OnClick(R.id.btn_recording_stop)
    fun stopRecording() = recordingViewModel.stopRecording()

    @OnClick(R.id.btn_audio_archive)
    fun openAudioArchives() {
        mainViewModel.checkStoragePermission()
    }

    @OnClick(R.id.btn_play_last_recording)
    fun playLastRecording() = recordingViewModel.playRecording()

    private fun setRecordingState(recordingStatus: Recorder.RecordingStatus) = when(recordingStatus){
            Recorder.RecordingStatus.recording -> {
                btnRecordAction.setImageDrawable(context?.getDrawable(R.drawable.bg_recording_action_pause))
                tvRecordAction.setText(R.string.pause)
                tvRecording.setText(R.string.recording)
                sineWaveView.startAnimation()
            }
            Recorder.RecordingStatus.paused -> {
                btnRecordAction.setImageDrawable(context?.getDrawable(R.drawable.bg_recording_action_record))
                tvRecordAction.setText(R.string.resume)
                tvRecording.setText(R.string.paused)
                sineWaveView.stopAnimation()
            }
            Recorder.RecordingStatus.ended -> {
                btnRecordAction.setImageDrawable(context?.getDrawable(R.drawable.bg_recording_action_record))
                recordingViewModel.setStateInitial()
            }
            else -> {}
        }

    private fun setInitialLayout() {
        groupRecording.visibility = View.INVISIBLE
        btnStartRecording.visibility = View.VISIBLE
        btnAudioArchive.visibility = View.VISIBLE

        tvRecording.setText(R.string.start_recording)

        sineWaveView.visibility = View.VISIBLE
        sineWaveView.baseWaveAmplitudeScale = 0f
        sineWaveView.stopAnimation()

        recordingViewModel.setLastRecordingControls()
    }

    private fun setRecordingLayout(){
        btnStartRecording.visibility = View.INVISIBLE
        btnAudioArchive.visibility = View.INVISIBLE
        groupRecording.visibility = View.VISIBLE

        tvRecording.setText(R.string.recording)

        sineWaveView.visibility = View.VISIBLE
        sineWaveView.startAnimation()
    }
}
