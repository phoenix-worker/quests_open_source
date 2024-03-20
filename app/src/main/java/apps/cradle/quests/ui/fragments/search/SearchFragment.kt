package apps.cradle.quests.ui.fragments.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentSearchBinding
import apps.cradle.quests.ui.adapters.SearchItemsAdapter
import apps.cradle.quests.ui.dialogs.SelectQuestDialog
import apps.cradle.quests.ui.dialogs.SlideToPerformDialog
import apps.cradle.quests.ui.dialogs.closedTaskDialog.ClosedTaskDialog
import apps.cradle.quests.ui.fragments.notes.note.NoteFragment
import apps.cradle.quests.ui.fragments.tasks.task.TaskFragment
import apps.cradle.quests.utils.EMPTY
import apps.cradle.quests.utils.Locator
import apps.cradle.quests.utils.NotesUtils
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.addDefaultItemDecorations
import apps.cradle.quests.utils.showKeyboard

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private val searchVM by activityViewModels<SearchViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        setupEmptyView()
        setupRecyclerView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
    }

    override fun onStart() {
        super.onStart()
        requireActivity().window
            .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        binding.searchClause.setText(searchVM.searchClause)
        if (searchVM.searchClause.isEmpty()) showKeyboard(binding.searchClause, requireActivity())
    }

    private fun setObservers() {
        searchVM.searchItems.observe(viewLifecycleOwner) { onSearchItemChanged(it) }
        Locator.conditionsChanged.observe(viewLifecycleOwner) {
            searchVM.search(binding.searchClause.text.toString())
        }
    }

    private fun onSearchItemChanged(searchItems: List<SearchItemsAdapter.Searchable>) {
        (binding.recyclerView.adapter as SearchItemsAdapter).submitList(searchItems)
        if (searchItems.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.root.visibility = View.VISIBLE
        } else {
            binding.emptyView.root.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setListeners() {
        binding.run {
            searchClause.doOnTextChanged { searchClause, _, _, _ -> doOnTextChanged(searchClause) }
            clear.setOnClickListener {
                binding.searchClause.setText(EMPTY)
                showKeyboard(binding.searchClause, requireActivity())
            }
        }
    }

    private fun doOnTextChanged(searchClause: CharSequence?) {
        binding.emptyView.run {
            if (searchClause.isNullOrBlank()) {
                image.setImageResource(R.drawable.image_search)
                title.text = getString(R.string.searchEmptyViewTitle)
                hint.text = getString(R.string.searchEmptyViewHint)
            } else {
                image.setImageResource(R.drawable.image_empty)
                title.text = getString(R.string.searchEmptyViewTitleNoResults)
                hint.text = getString(R.string.searchEmptyViewHintNoResults)
            }
            if (searchClause.isNullOrEmpty()) binding.clear.visibility = View.INVISIBLE
            else binding.clear.visibility = View.VISIBLE
        }
        searchVM.search(searchClause?.toString().orEmpty())
    }

    private fun setupEmptyView() {
        binding.emptyView.run {
            image.setImageResource(R.drawable.image_search)
            title.text = getString(R.string.searchEmptyViewTitle)
            hint.text = getString(R.string.searchEmptyViewHint)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = SearchItemsAdapter(
            onTaskClicked = onTaskClicked,
            onFinishedTaskClicked = onFinishedTaskClicked,
            onTaskLongClicked = onTaskLongClicked,
            onNoteClicked = onNoteClicked,
            onMoveNoteClicked = onMoveNoteClicked
        )
        binding.recyclerView.addDefaultItemDecorations()
    }

    private val onFinishedTaskClicked: (String) -> Unit = { taskId ->
        val bundle = Bundle()
        bundle.putString(ClosedTaskDialog.EXTRA_TASK_ID, taskId)
        ClosedTaskDialog().apply {
            arguments = bundle
            setOnDeleteTaskClicked(onDeleteClosedTaskClicked)
        }.show(childFragmentManager, "closed_task_dialog")
    }

    private val onDeleteClosedTaskClicked: (String) -> Unit = { taskId ->
        SlideToPerformDialog(
            titleText = getString(R.string.deleteQuickTaskDialogTitle),
            messageText = getString(R.string.deleteQuickTaskDialogMessage),
            action = getString(R.string.delete),
            onSliderFinishedListener = {
                TasksUtils.deleteTaskAndAllItsData(taskId)
            }
        ).show(childFragmentManager, "delete_task_dialog")
    }

    private val onTaskClicked: (String) -> Unit = { taskId ->
        val bundle = Bundle()
        bundle.putString(TaskFragment.EXTRA_TASK_ID, taskId)
        findNavController().navigate(R.id.action_searchFragment_to_taskFragment, bundle)
    }

    private val onTaskLongClicked: (String, String) -> Unit = { questId, taskId ->
        SelectQuestDialog().apply {
            setCurrentQuestId(questId)
            setOnQuestSelected { newQuestId ->
                TasksUtils.changeTaskQuest(taskId, newQuestId)
                searchVM.search(binding.searchClause.text.toString())
            }
            customizeDialog(
                title = this@SearchFragment.getString(R.string.cqdTransferTitle),
                buttonText = this@SearchFragment.getString(R.string.cqdTransferButtonText),
                emptyViewTitle = this@SearchFragment.getString(R.string.cqdTransferEmptyViewTitle),
                emptyViewHint = this@SearchFragment.getString(R.string.cqdTransferEmptyViewHint),
            )
        }.show(childFragmentManager, "change_quest_dialog")
    }

    private val onNoteClicked: (String) -> Unit = { noteId ->
        val bundle = Bundle()
        bundle.putString(NoteFragment.EXTRA_NOTE_ID, noteId)
        findNavController().navigate(R.id.action_searchFragment_to_noteFragment, bundle)
    }

    private val onMoveNoteClicked: (String, String) -> Unit = { questId, noteId ->
        SelectQuestDialog().apply {
            setCurrentQuestId(questId)
            setOnQuestSelected { newQuestId ->
                NotesUtils.changeNoteQuest(noteId, newQuestId)
                searchVM.search(binding.searchClause.text.toString())
            }
            customizeDialog(
                title = this@SearchFragment.getString(R.string.cqdTransferTitle),
                buttonText = this@SearchFragment.getString(R.string.cqdTransferButtonText),
                emptyViewTitle = this@SearchFragment.getString(R.string.cqdTransferEmptyViewTitle),
                emptyViewHint = this@SearchFragment.getString(R.string.cqdTransferEmptyViewHint),
            )
        }.show(childFragmentManager, "change_quest_dialog")
    }
}