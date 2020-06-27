package com.android.shaheer.recording.record


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.Group
import androidx.core.content.res.ResourcesCompat.getColor
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.android.shaheer.recording.MainViewModel
import com.android.shaheer.recording.MainViewModelFactory
import com.android.shaheer.recording.R
import com.android.shaheer.recording.databinding.FragmentRecordingBinding
import com.android.shaheer.recording.databinding.FragmentViewRecordsBinding
import com.android.shaheer.recording.dialogs.configs.ConfigsDialog
import com.android.shaheer.recording.services.RecordingService
import com.android.shaheer.recording.utils.*
import com.omega_r.libs.OmegaCenterIconButton
import kotlinx.android.synthetic.main.fragment_recording.*
import kotlinx.android.synthetic.main.row_record.*
import java.lang.IllegalArgumentException


class RecordingFragment : Fragment(), ConfigsDialog.OnCloseConfigsDialogListener {

    enum class RecordingStatus {
        notRecording, recording
    }

    private var _binding: FragmentRecordingBinding? = null
    private val binding get() = _binding!!

    private lateinit var recordingViewModel: RecordingViewModel
    private lateinit var mainViewModel: MainViewModel

    private var configsDialog: ConfigsDialog? = null

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        //inflater.inflate(R.layout.fragment_recording, container, false)
        _binding = FragmentRecordingBinding.inflate(inflater, container, false)
        binding.viewmodel = recordingViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stroke = CommonMethods.dipToPixels(context, 2f)
        binding.viewSineWave.addWave(0.5f, 0.5f, 0f, 0, 0f) // Fist wave is for the shape of other waves.
        binding.viewSineWave.addWave(0.5f, 6f, 1f, getColor(resources, R.color.wave, requireActivity().theme), stroke)


        binding.btnStartRecording.setOnClickListener{mainViewModel.checkPermissions()}

//        binding.btnRecordingAction.setOnClickListener { recordingViewModel.onRecordingAction() }

//        btn_recording_stop.setOnClickListener { recordingViewModel.stopRecording() }

        binding.btnAudioArchive.setOnClickListener { mainViewModel.checkStoragePermission()}

        binding.btnConfigs.setOnClickListener {
            configsDialog = ConfigsDialog(requireContext(), this)
            configsDialog?.show()
        }

//        btn_play_last_recording.setOnClickListener { recordingViewModel.playRecording() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        recordingViewModel.bindService.observe(viewLifecycleOwner, EventObserver {
            val intent = Intent(context, RecordingService::class.java)
            context?.bindService(intent, it, Context.BIND_AUTO_CREATE)
        })

        recordingViewModel.unbindService.observe(viewLifecycleOwner, EventObserver {
            try { context?.unbindService(it) }
            catch (e: IllegalArgumentException){e.printStackTrace()}
        })

        recordingViewModel.isServiceBound.observe(viewLifecycleOwner, EventObserver{/*TODO: do something*/})

        recordingViewModel.state.observe(viewLifecycleOwner, Observer{
            when(it){
                RecordingViewModel.RecordingState.NotRecording -> setInitialLayout()
                RecordingViewModel.RecordingState.Recording -> setRecordingLayout()
                RecordingViewModel.RecordingState.Paused -> setPausedLayout()
            }
        })

//        recordingViewModel.showLastRecordingButton.observe(viewLifecycleOwner, EventObserver{
//            if(it) btn_play_last_recording.visibility = View.VISIBLE
//            else btn_play_last_recording.visibility = View.INVISIBLE
//        })

//        recordingViewModel.duration.observe(viewLifecycleOwner, Observer { tv_record_duration.text = it  })
        recordingViewModel.amplitude.observe(viewLifecycleOwner, Observer { binding.viewSineWave.baseWaveAmplitudeScale = it  })

        recordingViewModel.playRecord.observe(viewLifecycleOwner, EventObserver {
            mainViewModel.playRecord(it)
        })

        recordingViewModel.showErrorToast.observe(viewLifecycleOwner, EventObserver{
            context?.showToast(it, Toast.LENGTH_SHORT)
        })

        mainViewModel.hasAllPermissions.observe(viewLifecycleOwner, EventObserver{
            if(it){
                val intent = Intent(context, RecordingService::class.java)
                //intent.action = RecordingService.ACTION_START
                context?.startService(intent)

                recordingViewModel.bindService()
            }
        })

        mainViewModel.hasStoragePermission.observe(viewLifecycleOwner, EventObserver{
            if(it){
                findNavController().navigate(
                        RecordingFragmentDirections.actionRecordingToViewRecords()
                )
            }
        })

    }

    override fun onPause() {
        super.onPause()

        if (CommonMethods.isServiceRunning(RecordingService::class.java, context)) {
            recordingViewModel.unbindService()
        }

        if(configsDialog != null && configsDialog?.isShowing == true){
            configsDialog?.dismiss()
        }

        binding.viewSineWave.stopAnimation()
    }

    override fun onResume() {
        super.onResume()

        if (CommonMethods.isServiceRunning(RecordingService::class.java, context)) recordingViewModel.bindService()
        else recordingViewModel.setStateInitial()
    }

    override fun onCloseConfigsDialog() {
        configsDialog = null
    }

    private fun setInitialLayout() {
        binding.groupRecording.visibility = View.INVISIBLE
        binding.btnStartRecording.visibility = View.VISIBLE
        binding.btnAudioArchive.visibility = View.VISIBLE
        binding.btnConfigs.visibility = View.VISIBLE

        binding.btnRecordingAction.background = context?.getDrawable(R.drawable.bg_recording_action_record)
        binding.tvStatus.setText(R.string.start_recording)

        binding.viewSineWave.stopAnimation()

        recordingViewModel.setLastRecordingControls()
    }

    private fun setRecordingLayout(){
        binding.btnStartRecording.visibility = View.INVISIBLE
        binding.btnAudioArchive.visibility = View.INVISIBLE
        binding.btnConfigs.visibility = View.INVISIBLE
        binding.groupRecording.visibility = View.VISIBLE

        binding.btnRecordingAction.background = context?.getDrawable(R.drawable.bg_recording_action_pause)
        binding.tvRecordingAction.setText(R.string.pause)
        binding.tvStatus.setText(R.string.recording)
        binding.viewSineWave.startAnimation()
    }

    private fun setPausedLayout(){
        binding.btnStartRecording.visibility = View.INVISIBLE
        binding.btnAudioArchive.visibility = View.INVISIBLE
        binding.btnConfigs.visibility = View.INVISIBLE
        binding.groupRecording.visibility = View.VISIBLE

        binding.btnRecordingAction.background = context?.getDrawable(R.drawable.bg_recording_action_record)
        binding.tvRecordingAction.setText(R.string.resume)
        binding.tvStatus.setText(R.string.paused)
        binding.viewSineWave.stopAnimation()
    }
}
