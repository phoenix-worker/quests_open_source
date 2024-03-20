package apps.cradle.quests.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.databinding.DialogChangeQuestBinding
import apps.cradle.quests.models.Category
import apps.cradle.quests.ui.fragments.categories.CategoriesFragment
import apps.cradle.quests.ui.fragments.categories.CategoriesViewModel
import apps.cradle.quests.utils.events.EventObserver
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SelectQuestDialog : BaseDialog() {

    private lateinit var binding: DialogChangeQuestBinding
    private val categoriesVM by activityViewModels<CategoriesViewModel>()

    private var currentQuestId: String? = null
    private lateinit var onQuestSelected: ((String) -> Unit)
    private var title: String? = null
    private var buttonText: String? = null
    private var emptyViewTitle: String? = null
    private var emptyViewHint: String? = null

    fun customizeDialog(
        title: String,
        buttonText: String,
        emptyViewTitle: String,
        emptyViewHint: String
    ) {
        this.title = title
        this.buttonText = buttonText
        this.emptyViewTitle = emptyViewTitle
        this.emptyViewHint = emptyViewHint
    }

    fun setCurrentQuestId(currentQuestId: String) {
        this.currentQuestId = currentQuestId
    }

    fun setOnQuestSelected(onQuestSelected: (String) -> Unit) {
        this.onQuestSelected = onQuestSelected
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogChangeQuestBinding.inflate(inflater, container, false)
        setupEmptyView()
        applyCustomStrings()
        return binding.root
    }

    private fun applyCustomStrings() {
        title?.let { binding.title.text = it }
            ?: throw IllegalArgumentException("Must have title.")
        buttonText?.let { binding.buttonHeap.text = it }
            ?: throw IllegalArgumentException("Must have button text.")
        emptyViewTitle?.let { binding.emptyView.title.text = it }
            ?: throw IllegalArgumentException("Must have empty view title.")
        emptyViewHint?.let { binding.emptyView.hint.text = it }
            ?: throw IllegalArgumentException("Must have empty view hint.")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        setupViewPager()
        categoriesVM.updateCategories(showEmpty = false)
        val shouldLeave = runBlocking(Dispatchers.IO) {
            val quests = App.db.questsDao().getAll()
            val isCurrentQuestIsSingleAndIsNotHeap =
                quests.size == 2 && currentQuestId != DbQuest.HEAP_QUEST_ID && currentQuestId != null
            val isOnlyHeapAvailable = quests.size == 1
            if (isCurrentQuestIsSingleAndIsNotHeap || isOnlyHeapAvailable) {
                binding.viewPager.isVisible = false
                binding.tabLayout.isVisible = false
                binding.emptyView.root.isVisible = true
            }
            quests.size <= 1 && currentQuestId != null
        }
        if (shouldLeave) {
            dismiss()
            Toast.makeText(
                requireActivity(),
                getString(R.string.toastNoQuestsForChangingQuest),
                Toast.LENGTH_LONG
            ).show()
        }
        binding.buttonHeap.setOnClickListener { onQuestSelected(DbQuest.HEAP_QUEST_ID) }
        binding.buttonHeap.isVisible = currentQuestId != DbQuest.HEAP_QUEST_ID
    }

    private fun setObservers() {
        categoriesVM.run {
            categories.observe(viewLifecycleOwner) { onCategoriesChanged(it) }
            questClickedEvent.observe(viewLifecycleOwner, onQuestClickedEvent)
        }
    }

    private val onQuestClickedEvent = EventObserver<String> {
        onQuestSelected(it)
    }

    private fun onQuestSelected(questId: String) {
        onQuestSelected.invoke(questId)
        dismiss()
    }

    private fun onCategoriesChanged(categories: List<Category>) {
        (binding.viewPager.adapter as CategoriesFragment.ViewPagerAdapter).updateData(categories)
    }

    private fun setupViewPager() {
        val adapter = CategoriesFragment.ViewPagerAdapter(
            this,
            CategoriesFragment.ViewPagerAdapter.QuestsPagerMode.SELECTOR,
            questIdToExclude = currentQuestId
        )
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = adapter.getData()[position].title
        }.attach()
    }

    private fun setupEmptyView() {
        binding.emptyView.apply {
            image.setImageResource(R.drawable.image_empty)
            root.isVisible = false
        }
    }

}