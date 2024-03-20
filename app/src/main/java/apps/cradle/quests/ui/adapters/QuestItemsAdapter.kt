package apps.cradle.quests.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.databinding.RvQuestElementBinding
import apps.cradle.quests.models.QuestElement
import apps.cradle.quests.models.QuestItem

class QuestItemsAdapter(
    private val onQuestClicked: (String) -> Unit,
    private val onQuestLongClicked: ((String, String) -> Unit)? = null
) : ListAdapter<QuestItem, QuestItemsAdapter.QuestItemVH>(QuestItemDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestItemVH {
        return when (viewType) {
            VIEW_TYPE_QUEST_ELEMENT -> QuestElementVH.getViewHolder(
                parent,
                onQuestClicked,
                onQuestLongClicked
            )

            else -> throw IllegalArgumentException("Wrong QuestItem view type.")
        }
    }

    override fun onBindViewHolder(holder: QuestItemVH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is QuestElement -> VIEW_TYPE_QUEST_ELEMENT
        }
    }

    sealed class QuestItemVH(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(questItem: QuestItem)

    }

    class QuestElementVH(
        private val binding: RvQuestElementBinding,
        private val onQuestClicked: (String) -> Unit,
        private val onQuestLongClicked: ((String, String) -> Unit)?
    ) : QuestItemVH(binding.root) {

        val white = ContextCompat.getColor(App.instance, R.color.textPrimaryWhite)
        val hint = ContextCompat.getColor(App.instance, R.color.textHintWhite)

        override fun bind(questItem: QuestItem) {
            binding.root.apply {
                binding.root.setQuest(questItem as QuestElement)
                setOnClickListener {
                    onQuestClicked.invoke(questItem.id)
                }
                setOnLongClickListener {
                    onQuestLongClicked?.invoke(questItem.id, questItem.categoryId)
                    true
                }
                binding.root.isLongClickable = onQuestLongClicked != null
            }
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                onQuestClicked: (String) -> Unit,
                onQuestLongClicked: ((String, String) -> Unit)?
            ): QuestElementVH {
                val binding = RvQuestElementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return QuestElementVH(binding, onQuestClicked, onQuestLongClicked)
            }

        }

    }

    class QuestItemDiffUtilCallback : DiffUtil.ItemCallback<QuestItem>() {

        override fun areItemsTheSame(oldItem: QuestItem, newItem: QuestItem): Boolean {
            return when {
                oldItem is QuestElement && newItem is QuestElement -> {
                    return oldItem.id == newItem.id
                }

                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: QuestItem, newItem: QuestItem): Boolean {
            return when {
                oldItem is QuestElement && newItem is QuestElement -> {
                    return oldItem == newItem
                }

                else -> false
            }
        }

    }

    companion object {

        const val VIEW_TYPE_QUEST_ELEMENT = 1

    }

}