package apps.cradle.quests.ui.fragments.notes.newNote

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
import apps.cradle.quests.databinding.FragmentEditNoteBinding
import apps.cradle.quests.ui.dialogs.ConfirmationDialog
import apps.cradle.quests.utils.hideKeyboard
import apps.cradle.quests.utils.showKeyboard

class NewNoteFragment : Fragment() {

    private lateinit var binding: FragmentEditNoteBinding
    private val newNoteVM by viewModels<NewNoteViewModel>()
    private var prevStatusBarColor = 0
    private var sheetColor = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditNoteBinding.inflate(inflater, container, false)
        binding.title.setRawInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        prevStatusBarColor = requireActivity().window.statusBarColor
        sheetColor = ContextCompat.getColor(requireActivity(), R.color.sheetBackground)
        showKeyboard(binding.title, requireActivity())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
        newNoteVM.exitEvent.observe(viewLifecycleOwner) { findNavController().popBackStack() }
        arguments?.getString(EXTRA_QUEST_ID)?.let { newNoteVM.loadQuestTitle(it) }
    }

    private fun setObservers() {
        newNoteVM.questTitle.observe(viewLifecycleOwner) { binding.toolbar.subtitle = it }
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
            R.id.done -> createNote()
        }
        true
    }

    private fun setupMenu() {
        val hasTitle = !binding.title.text.isNullOrBlank()
        val hasContent = !binding.content.text.isNullOrBlank()
        val canCreateNote = hasTitle && hasContent
        val hasDoneItem = binding.toolbar.menu.findItem(R.id.done) != null
        if (!canCreateNote) binding.toolbar.menu.clear()
        else if (!hasDoneItem) binding.toolbar.inflateMenu(R.menu.fragment_note)
        binding.toolbar.menu.removeItem(R.id.delete)
    }

    private fun confirmExit() {
        val hasTitle = !binding.title.text.isNullOrBlank()
        val hasContent = !binding.content.text.isNullOrBlank()
        if (!hasTitle && !hasContent) findNavController().popBackStack()
        else {
            ConfirmationDialog(
                title = getString(R.string.confirmNoteCancelingDialogTitle),
                message = getString(R.string.confirmNoteCancelingDialogMessage),
                positiveButtonText = getString(R.string.buttonYes),
                negativeButtonText = getString(R.string.buttonNo),
                onPositiveButtonClicked = { findNavController().popBackStack() }
            ).show(childFragmentManager, "confirm_exit_dialog")
        }
    }

    private fun createNote() {
        arguments?.getString(EXTRA_QUEST_ID)?.let { questId ->
            newNoteVM.createNote(
                questId = questId,
                noteTitle = binding.title.text.toString(),
                noteContent = binding.content.text.toString()
            )
        }
    }

    companion object {

        const val EXTRA_QUEST_ID = "extra_quest_id"

    }

}