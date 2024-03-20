package apps.cradle.quests.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.databinding.RvNoteBinding
import apps.cradle.quests.models.DataItem
import apps.cradle.quests.models.NoteElement
import apps.cradle.quests.utils.Locator

class DataItemsAdapter(
    private val onNoteClicked: ((String) -> Unit)?,
    private val onMoveNoteClicked: ((String, String) -> Unit)?,
) : ListAdapter<DataItem, DataItemsAdapter.DataItemVH>(DataItemDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataItemVH {
        return when (viewType) {
            VIEW_TYPE_NOTE -> NoteVH.getViewHolder(
                parent,
                onNoteClicked,
                onMoveNoteClicked
            )

            else -> throw IllegalArgumentException("Wrong view type.")
        }
    }

    override fun onBindViewHolder(holder: DataItemVH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NoteElement -> VIEW_TYPE_NOTE
            else -> throw IllegalArgumentException("Wrong DataItem child.")
        }
    }

    sealed class DataItemVH(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(dataItem: DataItem)

    }

    class NoteVH(
        private val binding: RvNoteBinding,
        private val onNoteClicked: ((String) -> Unit)?,
        private val onMoveNoteClicked: ((String, String) -> Unit)?
    ) : DataItemVH(binding.root), SearchItemsAdapter.SearchableVH {

        override fun bind(dataItem: DataItem) {
            (dataItem as NoteElement).let { note ->
                binding.apply {
                    title.text = note.title
                    content.text = note.content
                    quest.text = note.questTitle
                    questInfo.isVisible = false
                    root.setOnClickListener { onNoteClicked?.invoke(note.id) }
                    root.setOnLongClickListener {
                        onMoveNoteClicked?.invoke(note.questId, note.id)
                        true
                    }
                }
            }
        }

        override fun bindSearchable(item: SearchItemsAdapter.Searchable) {
            (item as NoteElement).let { note ->
                bind(note)
                binding.quest.setOnClickListener {
                    Locator.sendOpenQuestEvent(note.questId)
                }
                binding.questInfo.isVisible = true
                binding.archiveIcon.isVisible = note.isArchived
            }
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                onNoteClicked: ((String) -> Unit)?,
                onMoveNoteClicked: ((String, String) -> Unit)?,
            ): NoteVH {
                val binding = RvNoteBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return NoteVH(binding, onNoteClicked, onMoveNoteClicked)
            }

        }

    }

    class DataItemDiffUtilCallback : DiffUtil.ItemCallback<DataItem>() {

        override fun areItemsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return when {
                oldItem is NoteElement && newItem is NoteElement -> oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DataItem, newItem: DataItem): Boolean {
            return when {
                oldItem is NoteElement && newItem is NoteElement -> oldItem == newItem
                else -> false
            }
        }
    }

    companion object {

        const val VIEW_TYPE_NOTE = 0

    }

}