package apps.cradle.quests.ui.fragments.quests.quest

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.databinding.FragmentQuestBinding
import apps.cradle.quests.models.Quest
import apps.cradle.quests.ui.dialogs.ChangeCategoryDialog
import apps.cradle.quests.ui.dialogs.InfoDialog
import apps.cradle.quests.ui.dialogs.SlideToPerformDialog
import apps.cradle.quests.ui.fragments.notes.newNote.NewNoteFragment
import apps.cradle.quests.ui.fragments.tasks.newTask.NewTaskFragment
import apps.cradle.quests.utils.Locator
import apps.cradle.quests.utils.events.EmptyEvent
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class QuestFragment : Fragment() {

    private lateinit var binding: FragmentQuestBinding
    private val questVM by activityViewModels<QuestViewModel>()
    private var sheetColor = 0
    private var backgroundColor = 0
    private val questId by lazy {
        arguments?.getString(EXTRA_QUEST_ID, null)
            ?: throw IllegalArgumentException("Wrong questId.")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuestBinding.inflate(inflater, container, false)
        setupViewPager()
        sheetColor = ContextCompat.getColor(requireActivity(), R.color.sheetBackground)
        backgroundColor = ContextCompat.getColor(requireActivity(), R.color.windowBackground)
        setupTitle(binding.title)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
    }

    override fun onStart() {
        super.onStart()
        requireActivity().window.statusBarColor = sheetColor
        questVM.initShowFinishedTasks(questId)
        questVM.update(questId)
    }

    override fun onStop() {
        super.onStop()
        requireActivity().window.statusBarColor = backgroundColor
        saveChanges()
    }

    override fun onDestroy() {
        super.onDestroy()
        questVM.clear()
    }

    private fun setListeners() {
        binding.run {
            toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
            toolbar.setOnMenuItemClickListener(onMenuItemClicked)
            fab.setOnClickListener { onFabClicked() }
        }
    }

    private fun onFabClicked() {
        when (binding.viewPager.currentItem) {
            0 -> onAddNewTaskClicked()
            1 -> onAddNewMaterialClicked()
        }
    }

    private val onMenuItemClicked: (MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.delete -> {
                SlideToPerformDialog(
                    titleText = getString(R.string.deleteQuestDialogTitle),
                    messageText = getString(R.string.deleteQuestDialogMessage),
                    action = getString(R.string.delete),
                    onSliderFinishedListener = {
                        questVM.deleteQuest(
                            arguments?.getString(
                                EXTRA_QUEST_ID,
                                null
                            )
                        )
                    }
                ).show(childFragmentManager, "delete_quest_dialog")
            }

            R.id.unarchive -> onUnarchivedClicked()
        }
        true
    }

    private fun onUnarchivedClicked() = runBlocking(Dispatchers.IO) {
        val categories = App.db.categoriesDao().getAll()
        if (categories.isNotEmpty()) {
            binding.root.handler.post {
                questVM.quest.value?.let { quest ->
                    ChangeCategoryDialog(
                        questId = quest.id,
                        oldQuestCategoryId = quest.categoryId,
                        title = getString(R.string.unarchiveQuestDialogTitle),
                        onCategoryChanged = { questVM.sendExitEvent() }
                    ).show(childFragmentManager, "change_category_dialog")
                }
            }
        } else {
            InfoDialog(
                title = getString(R.string.noCategoriesDialogTitle),
                message = getString(R.string.noCategoriesDialogMessage)
            ).show(childFragmentManager, "info_dialog")
        }
    }

    private fun onAddNewTaskClicked() {
        val bundle = Bundle()
        bundle.putString(NewTaskFragment.EXTRA_QUEST_ID, questVM.quest.value?.id)
        findNavController().navigate(R.id.action_questFragment_to_newTaskFragment, bundle)
    }

    private fun onAddNewMaterialClicked() {
        val bundle = Bundle()
        bundle.apply { putString(NewNoteFragment.EXTRA_QUEST_ID, questVM.quest.value?.id) }
        findNavController().navigate(R.id.action_questFragment_to_newNoteFragment, bundle)
    }

    private fun setObservers() {
        questVM.run {
            quest.observe(viewLifecycleOwner) { onQuestChanged(it) }
            categoryTitle.observe(viewLifecycleOwner) { binding.toolbar.subtitle = it }
            exitEvent.observe(
                viewLifecycleOwner,
                EmptyEvent.Observer { findNavController().popBackStack() }
            )
            contentScrolling.observe(viewLifecycleOwner) { onContentScrolling(it) }
        }
        Locator.conditionsChanged.observe(viewLifecycleOwner) { questVM.update(questId) }
    }

    private fun onContentScrolling(isScrolling: Boolean) {
        if (questVM.quest.value?.categoryId == DbCategory.ARCHIVE_CATEGORY) return
        when (isScrolling) {
            true -> {
                binding.fab.hide()
            }

            false -> {
                binding.fab.show()
            }
        }
    }

    private fun onQuestChanged(quest: Quest?) {
        quest?.let {
            binding.title.setText(it.title)
            binding.archiveTitle.text = it.title
            when {
                quest.categoryId == DbCategory.ARCHIVE_CATEGORY -> {
                    binding.toolbar.title = getString(R.string.questFragmentTitle)
                    binding.archiveTitle.isVisible = true
                    binding.titleInputLayout.isVisible = false
                    binding.fab.isVisible = false
                    binding.heapHintContainer.isVisible = false
                }

                quest.id == DbQuest.HEAP_QUEST_ID -> {
                    binding.toolbar.title = getString(R.string.heapQuestTitle)
                    binding.toolbar.menu.clear()
                    binding.archiveTitle.isVisible = false
                    binding.heapHintContainer.isVisible = true
                    binding.titleInputLayout.isVisible = false
                }

                else -> {
                    binding.toolbar.title = getString(R.string.questFragmentTitle)
                    binding.toolbar.menu.removeItem(R.id.unarchive)
                    binding.archiveTitle.isVisible = false
                    binding.titleInputLayout.isVisible = true
                    binding.fab.isVisible = true
                    binding.heapHintContainer.isVisible = false
                }
            }
        }
    }

    private fun saveChanges() {
        questVM.quest.value?.let {
            val title = binding.title.text.toString()
            val maxLength = resources.getInteger(R.integer.maxTitleTextLength)
            val shouldRename = title != it.title && title.isNotBlank() && title.length <= maxLength
            if (shouldRename) questVM.renameQuest(title)
        }
    }

    private fun setupViewPager() {
        val isHeap = arguments?.getString(EXTRA_QUEST_ID, null) == DbQuest.HEAP_QUEST_ID
        val adapter = ViewPagerAdapter(this, isHeap)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tabTasksTitle)
                1 -> getString(R.string.tabDataTitle)
                else -> throw IllegalArgumentException("Wrong tab position.")
            }
        }.attach()
    }

    class ViewPagerAdapter(
        fragment: Fragment,
        private val isHeapQuest: Boolean
    ) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle()
            bundle.putBoolean(EXTRA_IS_HEAP, isHeapQuest)
            return when (position) {
                0 -> TasksFragment().apply { arguments = bundle }
                else -> DataFragment().apply { arguments = bundle }
            }
        }

    }

    companion object {

        const val EXTRA_QUEST_ID = "extra_quest_id"
        const val EXTRA_IS_HEAP = "extra_is_heap"

        fun setupTitle(title: EditText) {
            title.imeOptions = EditorInfo.IME_ACTION_DONE
            title.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        }
    }

}