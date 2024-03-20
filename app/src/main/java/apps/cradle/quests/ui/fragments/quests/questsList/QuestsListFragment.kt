package apps.cradle.quests.ui.fragments.quests.questsList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import apps.cradle.quests.R
import apps.cradle.quests.databinding.FragmentQuestsListBinding
import apps.cradle.quests.models.QuestElement
import apps.cradle.quests.models.QuestItem
import apps.cradle.quests.ui.adapters.QuestItemsAdapter
import apps.cradle.quests.ui.dialogs.ChangeCategoryDialog
import apps.cradle.quests.ui.fragments.categories.CategoriesViewModel
import apps.cradle.quests.ui.fragments.quests.quest.QuestFragment
import apps.cradle.quests.utils.LinearRvItemDecorations
import apps.cradle.quests.utils.orThrowIAE

class QuestsListFragment : Fragment() {

    private lateinit var binding: FragmentQuestsListBinding
    private val questsListVM: QuestsListViewModel by viewModels()
    private val categoriesVM: CategoriesViewModel by activityViewModels()
    private val categoryId by lazy {
        arguments?.getString(EXTRA_CATEGORY_ID).orThrowIAE("Wrong quest categoryId.")
    }
    private val mode by lazy { arguments?.getString(EXTRA_MODE).orThrowIAE("Wrong mode.") }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuestsListBinding.inflate(inflater, container, false)
        setupEmptyView()
        setupRvPaddings()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        questsListVM.updateQuestsList(categoryId)
        setupRecyclerView()
        setObservers()
    }

    private fun setObservers() {
        questsListVM.questsItems.observe(viewLifecycleOwner) { onQuestItemsChanged(it) }
        categoriesVM.newQuestCreatedEvent.observe(viewLifecycleOwner) {
            questsListVM.updateQuestsList(categoryId)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            addItemDecoration(
                LinearRvItemDecorations(
                    sideMarginsDimension = R.dimen.smallMargin,
                    marginBetweenElementsDimension = null
                )
            )
            adapter = QuestItemsAdapter(
                onQuestClicked = onQuestClicked,
                onQuestLongClicked = onQuestLongClicked
            )
            addOnScrollListener(object : OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        categoriesVM.sendQuestsScrolling(false)
                    else categoriesVM.sendQuestsScrolling(true)
                }
            })
        }
    }

    private val onQuestLongClicked: (String, String) -> Unit = { questId, oldQuestCategoryId ->
        when (mode) {
            MODE_COMMON -> {
                if ((categoriesVM.categories.value?.size ?: 1) > 1) {
                    ChangeCategoryDialog(
                        questId = questId,
                        oldQuestCategoryId = oldQuestCategoryId,
                        title = getString(R.string.changeCategoryDialogTitle)
                    ).show(childFragmentManager, "change_category_dialog")
                }
            }

            else -> {}
        }
    }

    private val onQuestClicked: (String) -> Unit = { questId ->
        when (mode) {
            MODE_COMMON -> {
                val bundle = Bundle().apply { putString(QuestFragment.EXTRA_QUEST_ID, questId) }
                findNavController().navigate(
                    R.id.action_categoriesFragment_to_questFragment,
                    bundle
                )
            }

            MODE_SELECTOR -> {
                categoriesVM.sendQuestClickedEvent(questId)
            }
        }
    }

    private fun onQuestItemsChanged(questItems: List<QuestItem>) {
        if (questItems.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.root.visibility = View.VISIBLE
        } else {
            binding.emptyView.root.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
        val result = mutableListOf<QuestItem>()
        questItems.forEach {
            if (it is QuestElement) {
                if (it.id != arguments?.getString(EXTRA_QUEST_ID_TO_EXCLUDE)) result.add(it)
            } else result.add(it)
        }
        (binding.recyclerView.adapter as QuestItemsAdapter).submitList(result)
    }

    private fun setupEmptyView() {
        binding.emptyView.run {
            image.setImageResource(R.drawable.image_empty)
            title.text = getString(R.string.emptyViewTitle)
            hint.text = getString(R.string.categoryEmptyViewHint)
            val categoryId = arguments?.getString(EXTRA_CATEGORY_ID)
                ?: throw IllegalArgumentException("Wrong categoryId.")
            button.setOnClickListener { categoriesVM.deleteCategory(categoryId) }
            button.visibility = View.VISIBLE
        }
    }

    private fun setupRvPaddings() {
        val bottomPadding = when (mode) {
            MODE_COMMON -> resources.getDimension(R.dimen.hugeMargin)
            MODE_SELECTOR -> resources.getDimension(R.dimen.smallMargin)
            else -> throw IllegalArgumentException("Wrong mode.")
        }
        val topPadding = when (mode) {
            MODE_COMMON -> resources.getDimension(R.dimen.smallMargin)
            MODE_SELECTOR -> resources.getDimension(R.dimen.smallMargin)
            else -> throw IllegalArgumentException("Wrong mode.")
        }
        binding.recyclerView.updatePadding(
            top = topPadding.toInt(),
            bottom = bottomPadding.toInt()
        )
    }

    companion object {

        const val EXTRA_CATEGORY_ID = "extra_category_id"

        const val EXTRA_MODE = "EXTRA_MODE"
        const val MODE_COMMON = "mode_common"
        const val MODE_SELECTOR = "mode_selector"

        const val EXTRA_QUEST_ID_TO_EXCLUDE = "EXTRA_QUEST_ID_TO_EXCLUDE"

    }

}