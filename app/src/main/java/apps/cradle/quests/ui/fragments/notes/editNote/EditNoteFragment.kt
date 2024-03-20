package apps.cradle.quests.ui.fragments.notes.editNote

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbNote
import apps.cradle.quests.databinding.FragmentEditNoteBinding
import apps.cradle.quests.ui.dialogs.ConfirmationDialog
import apps.cradle.quests.ui.fragments.notes.note.NoteFragment.Companion.EXTRA_NOTE_ID
import apps.cradle.quests.utils.hideKeyboard

class EditNoteFragment : Fragment() {

    private lateinit var binding: FragmentEditNoteBinding
    private val noteVM by viewModels<EditNoteViewModel>()
    private var prevStatusBarColor = 0
    private var sheetColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        binding.toolbar.title = getString(R.string.editTitle)
        binding.title.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        prevStatusBarColor = requireActivity().window.statusBarColor
        sheetColor = ContextCompat.getColor(requireActivity(), R.color.sheetBackground)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
        arguments?.getString(EXTRA_NOTE_ID)?.let { noteVM.initialize(it) }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().window.statusBarColor = sheetColor
        setupMenu()
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { confirmExit() }
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.statusBarColor = prevStatusBarColor
        hideKeyboard(requireActivity().window.decorView.windowToken, requireActivity())
    }

    private fun setObservers() {
        noteVM.run {
            exitEvent.observe(viewLifecycleOwner) { findNavController().popBackStack() }
            note.observe(viewLifecycleOwner) { onNoteChanged(it) }
        }
    }

    private fun onNoteChanged(dbNote: DbNote?) {
        dbNote?.let {
            binding.title.setText(it.title)
            binding.content.setText(it.content)
        }
    }

    private fun setListeners() {
        binding.run {
            toolbar.setNavigationOnClickListener { confirmExit() }
            toolbar.setOnMenuItemClickListener(onMenuItemClicked)
            title.doOnTextChanged { _, _, _, _ -> setupMenu() }
            content.doOnTextChanged { _, _, _, _ -> setupMenu() }
        }
    }

    private val onMenuItemClicked: (MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.done -> saveNote()
        }
        true
    }

    private fun setupMenu() {
        val hasDoneItem = binding.toolbar.menu.findItem(R.id.done) != null
        if (!canSaveNote()) binding.toolbar.menu.removeItem(R.id.done)
        else if (!hasDoneItem) {
            binding.toolbar.menu.clear()
            binding.toolbar.inflateMenu(R.menu.fragment_note)
        }
    }

    private fun confirmExit() {
        if (!canSaveNote()) findNavController().popBackStack()
        else {
            ConfirmationDialog(
                title = getString(R.string.saveChangesQuestion),
                message = getString(R.string.saveChangesNoteMessage),
                positiveButtonText = getString(R.string.buttonYes),
                negativeButtonText = getString(R.string.buttonNo),
                onPositiveButtonClicked = { saveNote() },
                onNegativeButtonClicked = { findNavController().popBackStack() }
            ).show(childFragmentManager, "confirm_exit_dialog")
        }
    }

    private fun canSaveNote(): Boolean {
        val hasTitle = !binding.title.text.isNullOrBlank()
        val hasContent = !binding.content.text.isNullOrBlank()
        val hasTitleChanges = binding.title.text.toString() != noteVM.note.value?.title
        val hasContentChanges = binding.content.text.toString() != noteVM.note.value?.content
        val hasChanges = hasTitleChanges || hasContentChanges
        return hasTitle && hasContent && hasChanges
    }

    private fun saveNote() {
        noteVM.updateNoteAndExit(
            title = binding.title.text.toString(),
            content = binding.content.text.toString()
        )
    }
}