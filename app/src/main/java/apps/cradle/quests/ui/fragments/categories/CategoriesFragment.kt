package apps.cradle.quests.ui.fragments.categories

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.adapter.FragmentStateAdapter
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.databinding.FragmentCategoriesBinding
import apps.cradle.quests.models.Category
import apps.cradle.quests.ui.dialogs.EnterValueDialog
import apps.cradle.quests.ui.fragments.quests.quest.QuestFragment
import apps.cradle.quests.ui.fragments.quests.questsList.QuestsListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class CategoriesFragment : Fragment() {

    private lateinit var binding: FragmentCategoriesBinding
    private val categoriesVM: CategoriesViewModel by activityViewModels()
    private var currentPage = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        setupEmptyView()
        setupViewPager()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
        setObservers()
    }

    override fun onStart() {
        super.onStart()
        categoriesVM.updateCategories()
    }

    override fun onStop() {
        super.onStop()
        currentPage = binding.viewPager.currentItem
    }

    private fun setListeners() {
        binding.run {
            toolbar.setOnMenuItemClickListener(onMenuItemClicked)
            newQuest.setOnClickListener { onFabNewQuestClicked() }
            heap.setOnClickListener { onHeapButtonClicked() }
            archive.setOnClickListener { onArchiveButtonClicked() }
        }
    }

    private fun setObservers() {
        categoriesVM.run {
            categories.observe(viewLifecycleOwner) { onCategoriesChanged(it) }
            activeQuestsCount.observe(viewLifecycleOwner) { onActiveQuestsCountChanged(it) }
            questsScrolling.observe(viewLifecycleOwner) { onQuestsScrolling(it) }
            hasArchive.observe(viewLifecycleOwner) { binding.archive.isVisible = it }
        }
    }

    private fun onArchiveButtonClicked() {
        findNavController().navigate(R.id.action_categoriesFragment_to_archiveFragment)
    }

    private fun onQuestsScrolling(areQuestsScrolling: Boolean) {
        when (areQuestsScrolling) {
            true -> {
                binding.newQuest.hide()
                binding.archive.hide()
                binding.heap.hide(100L)
            }

            false -> {
                binding.heap.show()
                binding.archive.show(100L)
                binding.newQuest.show()
            }
        }
    }

    private fun onHeapButtonClicked() {
        val bundle = Bundle()
        bundle.putString(QuestFragment.EXTRA_QUEST_ID, DbQuest.HEAP_QUEST_ID)
        findNavController().navigate(R.id.action_global_questFragment, bundle)
    }

    private fun onFabNewQuestClicked() {
        val adapter = binding.viewPager.adapter as ViewPagerAdapter
        val selectedCategoryId = adapter.getData()[binding.viewPager.currentItem].id
        EnterValueDialog(
            titleText = getString(R.string.newQuestDialogTitle),
            messageText = getString(R.string.newQuestDialogMessage),
            hintText = getString(R.string.newQuestDialogHint),
            negativeButtonText = getString(R.string.buttonCancel),
            positiveButtonText = getString(R.string.buttonCreate),
        ) { questTitle ->
            categoriesVM.createNewQuest(
                questTitle = questTitle,
                categoryId = selectedCategoryId
            )
        }.show(childFragmentManager, "new_quest_dialog")
    }

    private fun onActiveQuestsCountChanged(activeQuestsCount: Int) {
        binding.toolbar.subtitle = resources.getQuantityString(
            R.plurals.activeQuestsCount,
            activeQuestsCount,
            activeQuestsCount.toString()
        )
        if (activeQuestsCount == 0) binding.toolbar.subtitle = null
    }

    private val onMenuItemClicked: (MenuItem) -> Boolean = {
        when (it.itemId) {
            R.id.addCategory -> {
                EnterValueDialog(
                    titleText = getString(R.string.newCategoryDialogTitle),
                    messageText = getString(R.string.newCategoryDialogMessage),
                    hintText = getString(R.string.newCategoryDialogHint),
                    negativeButtonText = getString(R.string.buttonCancel),
                    positiveButtonText = getString(R.string.buttonCreate)
                ) { newCategoryTitle ->
                    categoriesVM.createNewCategory(newCategoryTitle)
                }.show(childFragmentManager, "new_category_dialog")
            }
        }
        true
    }

    private fun onCategoriesChanged(categories: List<Category>) {
        if (categories.isEmpty()) {
            binding.tabLayout.isVisible = false
            binding.viewPager.isVisible = false
            binding.newQuest.isVisible = false
            binding.emptyView.root.isVisible = true
        } else {
            binding.emptyView.root.isVisible = false
            binding.tabLayout.isVisible = true
            binding.viewPager.isVisible = true
            binding.newQuest.isVisible = true
            (binding.viewPager.adapter as ViewPagerAdapter).updateData(categories)
        }
        binding.viewPager.doOnLayout { binding.viewPager.setCurrentItem(currentPage, false) }
    }

    private fun setupViewPager() {
        val adapter = ViewPagerAdapter(this, ViewPagerAdapter.QuestsPagerMode.COMMON)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val textView = TextView(context).apply {
                id = android.R.id.text1
                setTextColor(ContextCompat.getColor(requireActivity(), R.color.tab_text))
                gravity = Gravity.CENTER
                val textSizePixels = resources.getDimension(R.dimen.textSizeSmall)
                val dp = (textSizePixels / resources.displayMetrics.density)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, dp)
                setOnLongClickListener { true }
                setOnClickListener { tab.select() }
            }
            tab.text = adapter.getData()[position].title
            tab.customView = textView
        }.attach()
        binding.tabLayout.addOnTabSelectedListener(OnTabSelectedListener(adapter))
    }

    private inner class OnTabSelectedListener(
        private val adapter: ViewPagerAdapter
    ) : TabLayout.OnTabSelectedListener {

        override fun onTabSelected(tab: TabLayout.Tab?) {
            (tab?.customView as TextView).setTextColor(
                ContextCompat.getColor(requireActivity(), R.color.windowBackground)
            )
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
            (tab?.customView as TextView).setTextColor(
                ContextCompat.getColor(requireActivity(), R.color.textPrimaryWhite)
            )
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            try {
                adapter.getData()[tab!!.position].let { category ->
                    EnterValueDialog(
                        titleText = getString(R.string.renameCategoryDialogTitle),
                        messageText = getString(R.string.renameCategoryDialogMessage),
                        hintText = getString(R.string.newCategoryDialogHint),
                        negativeButtonText = getString(R.string.buttonCancel),
                        positiveButtonText = getString(R.string.buttonSave),
                        initialInput = category.title
                    ) { newTitle ->
                        if (newTitle != category.title)
                            categoriesVM.renameCategory(category.id, newTitle)
                    }.show(childFragmentManager, "rename_category_dialog")
                }
            } catch (exc: Exception) {
                App.log(exc.toString())
            }
        }
    }

    private fun setupEmptyView() {
        binding.emptyView.apply {
            image.setImageResource(R.drawable.image_empty)
            title.text = getString(R.string.categoriesEmptyViewTitle)
            hint.text = getString(R.string.categoriesEmptyViewHint)
        }
    }

    class ViewPagerAdapter(
        fragment: Fragment,
        private val mode: QuestsPagerMode,
        private var questIdToExclude: String? = null
    ) : FragmentStateAdapter(fragment) {

        enum class QuestsPagerMode { COMMON, SELECTOR }

        private var _data: List<Category> = listOf()

        @SuppressLint("NotifyDataSetChanged")
        fun updateData(data: List<Category>) {
            this._data = data
            notifyDataSetChanged()
        }

        fun getData(): List<Category> = _data

        override fun getItemCount(): Int {
            return _data.size
        }

        override fun getItemId(position: Int): Long {
            return _data[position].viewPageId
        }

        override fun containsItem(itemId: Long): Boolean {
            return _data.firstOrNull { it.viewPageId == itemId } != null
        }

        override fun createFragment(position: Int): Fragment {
            val bundle = Bundle()
            bundle.putString(QuestsListFragment.EXTRA_CATEGORY_ID, _data[position].id)
            bundle.putString(
                QuestsListFragment.EXTRA_MODE,
                when (mode) {
                    QuestsPagerMode.COMMON -> QuestsListFragment.MODE_COMMON
                    QuestsPagerMode.SELECTOR -> QuestsListFragment.MODE_SELECTOR
                }
            )
            questIdToExclude?.let {
                bundle.putString(
                    QuestsListFragment.EXTRA_QUEST_ID_TO_EXCLUDE,
                    it
                )
            }
            return QuestsListFragment().apply { arguments = bundle }
        }
    }
}