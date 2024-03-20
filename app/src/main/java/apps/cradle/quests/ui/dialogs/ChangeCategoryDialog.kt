package apps.cradle.quests.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import apps.cradle.quests.databinding.DialogChangeCategoryBinding
import apps.cradle.quests.models.Category
import apps.cradle.quests.ui.adapters.CategoriesAdapter
import apps.cradle.quests.ui.fragments.categories.CategoriesViewModel
import apps.cradle.quests.utils.addDefaultItemDecorations

class ChangeCategoryDialog(
    private val questId: String,
    private val oldQuestCategoryId: String,
    private val title: String,
    private val onCategoryChanged: (() -> Unit)? = null
) : BaseDialog() {

    private lateinit var binding: DialogChangeCategoryBinding
    private val categoriesVM: CategoriesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogChangeCategoryBinding.inflate(inflater, container, false)
        setupRecyclerView()
        binding.title.text = title
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoriesVM.categories.observe(viewLifecycleOwner) { onCategoriesChanged(it) }
    }

    private fun onCategoriesChanged(categories: List<Category>?) {
        categories?.let { list ->
            val filteredList = list.filter { it.id != oldQuestCategoryId }
            (binding.recyclerView.adapter as CategoriesAdapter).submitList(filteredList)
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.adapter = CategoriesAdapter(onClick)
        binding.recyclerView.addDefaultItemDecorations()
    }

    private val onClick: (String) -> Unit = { categoryId ->
        categoriesVM.changeQuestCategory(questId = questId, categoryId = categoryId)
        onCategoryChanged?.invoke()
        dismiss()
    }
}