package apps.cradle.quests.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.databinding.RvCategoryBinding
import apps.cradle.quests.models.Category

class CategoriesAdapter(
    private val onClick: (String) -> Unit
) : ListAdapter<Category, CategoriesAdapter.CategoryVH>(CategoryDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryVH {
        return CategoryVH.getViewHolder(parent)
    }

    override fun onBindViewHolder(holder: CategoryVH, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    class CategoryVH(
        private val binding: RvCategoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(category: Category, onClick: (String) -> Unit) {
            binding.title.text = category.title
            binding.root.setOnClickListener { onClick.invoke(category.id) }
        }

        companion object {

            fun getViewHolder(parent: ViewGroup): CategoryVH {
                val binding = RvCategoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return CategoryVH(binding)
            }

        }

    }

    class CategoryDiffUtilCallback : DiffUtil.ItemCallback<Category>() {

        override fun areItemsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Category, newItem: Category): Boolean {
            return oldItem == newItem
        }

    }

}