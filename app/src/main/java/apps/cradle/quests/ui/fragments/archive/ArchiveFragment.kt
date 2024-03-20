package apps.cradle.quests.ui.fragments.archive

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentArchiveBinding
import apps.cradle.quests.models.QuestItem
import apps.cradle.quests.ui.adapters.QuestItemsAdapter
import apps.cradle.quests.ui.fragments.quests.quest.QuestFragment
import apps.cradle.quests.utils.LinearRvItemDecorations

class ArchiveFragment : Fragment() {

    private lateinit var binding: FragmentArchiveBinding
    private val archiveVM by viewModels<ArchiveViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentArchiveBinding.inflate(inflater, container, false)
        binding.recyclerView.apply {
            addItemDecoration(
                LinearRvItemDecorations(
                    sideMarginsDimension = R.dimen.smallMargin,
                    marginBetweenElementsDimension = null
                )
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setupRecyclerView()
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    override fun onStart() {
        super.onStart()
        archiveVM.updateArchive()
    }

    private fun setObservers() {
        archiveVM.archiveQuests.observe(viewLifecycleOwner) { onArchiveQuestsChanged(it) }
    }

    private fun onArchiveQuestsChanged(archiveQuests: List<QuestItem>) {
        binding.toolbar.subtitle = resources.getQuantityString(
            R.plurals.fragmentArchiveSubtitle,
            archiveQuests.size,
            archiveQuests.size.toString()
        )
        (binding.recyclerView.adapter as QuestItemsAdapter).submitList(archiveQuests)
        if (archiveQuests.isEmpty()) findNavController().popBackStack()
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            adapter = QuestItemsAdapter(
                onQuestClicked = onQuestClicked
            )
        }
    }

    private val onQuestClicked: (String) -> Unit = { questId ->
        val bundle = Bundle()
        bundle.putString(QuestFragment.EXTRA_QUEST_ID, questId)
        findNavController().navigate(R.id.action_archiveFragment_to_questFragment, bundle)
    }
}