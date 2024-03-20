package apps.cradle.quests.ui.fragments.quests.quest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentDataBinding
import apps.cradle.quests.models.DataItem
import apps.cradle.quests.ui.adapters.DataItemsAdapter
import apps.cradle.quests.ui.dialogs.SelectQuestDialog
import apps.cradle.quests.ui.fragments.notes.note.NoteFragment
import apps.cradle.quests.utils.NotesUtils
import apps.cradle.quests.utils.addDefaultItemDecorations

class DataFragment : Fragment() {

    private lateinit var binding: FragmentDataBinding
    private val questVM by activityViewModels<QuestViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDataBinding.inflate(inflater, container, false)
        setupEmptyView()
        setupRecyclerView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
    }

    private fun setObservers() {
        questVM.data.observe(viewLifecycleOwner) { onDataChanged(it) }
    }

    private fun onDataChanged(data: List<DataItem>?) {
        data?.let { dataItems ->
            when {
                dataItems.isEmpty() -> {
                    binding.recyclerView.visibility = View.GONE
                    binding.emptyView.root.visibility = View.VISIBLE
                }

                else -> {
                    (binding.recyclerView.adapter as DataItemsAdapter).submitList(dataItems)
                    binding.emptyView.root.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupEmptyView() {
        val isHeap = arguments?.getBoolean(QuestFragment.EXTRA_IS_HEAP) ?: false
        binding.emptyView.run {
            image.setImageResource(R.drawable.image_empty)
            title.text = getString(R.string.emptyViewTitle)
            hint.text = getString(
                if (!isHeap) R.string.dataFragmentEmptyViewHint
                else R.string.heapDataFragmentEmptyViewHint
            )
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = DataItemsAdapter(
            onNoteClicked = onNoteClicked,
            onMoveNoteClicked = if (!questVM.isQuestArchived) onMoveNoteClicked else null
        )
        binding.recyclerView.addDefaultItemDecorations()
        binding.recyclerView.addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                questVM.sendContentScrolling(newState != RecyclerView.SCROLL_STATE_IDLE)
            }
        })
    }

    private val onNoteClicked: (String) -> Unit = { noteId ->
        val bundle = Bundle()
        bundle.putString(NoteFragment.EXTRA_NOTE_ID, noteId)
        findNavController().navigate(R.id.action_questFragment_to_noteFragment, bundle)
    }

    private val onMoveNoteClicked: (String, String) -> Unit = { currentQuestId, noteId ->
        SelectQuestDialog().apply {
            setCurrentQuestId(currentQuestId)
            setOnQuestSelected { newQuestId ->
                NotesUtils.changeNoteQuest(noteId, newQuestId)
                questVM.update(currentQuestId)
            }
            customizeDialog(
                title = this@DataFragment.getString(R.string.cqdTransferTitle),
                buttonText = this@DataFragment.getString(R.string.cqdTransferButtonText),
                emptyViewTitle = this@DataFragment.getString(R.string.cqdTransferEmptyViewTitle),
                emptyViewHint = this@DataFragment.getString(R.string.cqdTransferEmptyViewHint),
            )
        }.show(childFragmentManager, "change_quest_dialog")
    }
}