package com.android.shaheer.recording.editrecordings

import android.content.Context
import android.os.Bundle
import android.text.Layout
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input

import com.android.shaheer.recording.R
import com.android.shaheer.recording.utils.EventObserver
import com.android.shaheer.recording.utils.SessionManager
import com.android.shaheer.recording.utils.makeSnackBar
import com.android.shaheer.recording.utils.showToast
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar

class ViewRecordsFragment : Fragment(), RecordingListAdapter.ItemInteractionListener {

    @BindView(R.id.rv_recordings) lateinit var rvRecordings: RecyclerView
    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar

    lateinit var renameMenuAction: MenuItem
    lateinit var deleteMenuAction: MenuItem

    private lateinit var viewModel: ViewRecordsViewModel

    private lateinit var materialDialog: MaterialDialog

    private val recordingAdapter = RecordingListAdapter(this)

    override fun onAttach(context: Context) {
        super.onAttach(context)

        viewModel = ViewModelProvider(
                viewModelStore,
                ViewRecordsViewModelFactory(SessionManager(context.applicationContext))
        ).get(ViewRecordsViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_view_records, container, false)
        ButterKnife.bind(this, view)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupMenuActionButtons()

        rvRecordings.adapter = recordingAdapter

        viewModel.getRecordings.observe(viewLifecycleOwner,
            EventObserver{ forced ->
                context?.let { viewModel.getRecordingsFromFiles(it, forced) }
            }
        )

        viewModel.recordings.observe(viewLifecycleOwner, Observer { recordingAdapter.submitList(it) })

        viewModel.selectedRecordings.observe(viewLifecycleOwner, Observer { onRecordingsSelected(it) })

        viewModel.renameItem.observe(viewLifecycleOwner, EventObserver{onRenameClicked(it)})

        viewModel.showNameAlreadyExistsToast.observe(viewLifecycleOwner, EventObserver{context?.showToast(R.string.name_already_exists)})

    }

    override fun onResume() {
        super.onResume()
        context?.let { viewModel.getRecordingsFromFiles(it) }
    }

    override fun onPause() {
        super.onPause()
        if(::materialDialog.isInitialized && materialDialog.isShowing) {
            materialDialog.cancel()
        }
        viewModel.stopPlayingItem()
    }

    private fun setupMenuActionButtons(){

        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        deleteMenuAction = toolbar.menu.findItem(R.id.action_delete)
        renameMenuAction = toolbar.menu.findItem(R.id.action_rename)

        renameMenuAction.setOnMenuItemClickListener {
            viewModel.renameSelectedItem()
            true
        }

        deleteMenuAction.setOnMenuItemClickListener {

            view?.makeSnackBar(
                getString(R.string.delete_contfirmation_msg),
                Snackbar.LENGTH_SHORT
            )?.run{
                this.setAction(R.string.yes) {
                    viewModel.deleteSelectedItems(context)
                }
                this.show()
            }
            true
        }
    }

    override fun onItemSelected(position: Int) {
        viewModel.onRecordItemSelected(position)
    }

    override fun onItemClicked(position: Int) {
        viewModel.onRecordItemClicked(position)
    }

    override fun onItemPlayClicked(position: Int) {
        context?.let { viewModel.onItemPlayClicked(it, position) }
    }

    private fun onRecordingsSelected(selectedCount: Int) = when{
        selectedCount == 1 -> {
            renameMenuAction.isVisible = true
            deleteMenuAction.isVisible = true
            (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
        }
        selectedCount > 1 -> {
            renameMenuAction.isVisible = false
            deleteMenuAction.isVisible = true
            (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_NO_SCROLL
        }
        else -> {
            renameMenuAction.isVisible = false
            deleteMenuAction.isVisible = false
            (toolbar.layoutParams as AppBarLayout.LayoutParams).scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        }
    }

    private fun onRenameClicked(recordItemName: String){

        context?.let {
            materialDialog = MaterialDialog(it).show {
                title(R.string.type_recording_name)
                input(hint = recordItemName){ dialog, text ->
                    viewModel.renameRecordingFile(context, text.toString(), recordItemName)
                }
                positiveButton(R.string.rename)
            }
        }
    }

}
