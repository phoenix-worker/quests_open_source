package apps.cradle.quests.ui.adapters

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.NoteElement
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.ui.adapters.SearchItemsAdapter.Searchable

class SearchItemsAdapter(
    private val onNoteClicked: (String) -> Unit,
    private val onMoveNoteClicked: (String, String) -> Unit,
    private val onTaskClicked: (String) -> Unit,
    private val onFinishedTaskClicked: (String) -> Unit,
    private val onTaskLongClicked: (String, String) -> Unit,
) : ListAdapter<Searchable, RecyclerView.ViewHolder>(SearchItemDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_NOTE -> DataItemsAdapter.NoteVH.getViewHolder(
                parent,
                onNoteClicked = onNoteClicked,
                onMoveNoteClicked = onMoveNoteClicked
            )

            VIEW_TYPE_TASK_ACTIVE -> TaskItemsAdapter.TaskElementVH.getViewHolder(
                parent,
                TaskItemsAdapter.MODE.SEARCH,
                onTaskClicked,
                onTaskLongClicked
            )

            VIEW_TYPE_TASK_FINISHED -> TaskItemsAdapter.FinishedTaskElementVH.getViewHolder(
                parent,
                onFinishedTaskClicked
            )

            else -> throw IllegalArgumentException("Wrong view type.")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as SearchableVH).bindSearchable(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is NoteElement -> VIEW_TYPE_NOTE
            is TaskElement -> when (item.state) {
                DbTask.STATE_ACTIVE -> VIEW_TYPE_TASK_ACTIVE
                DbTask.STATE_FINISHED -> VIEW_TYPE_TASK_FINISHED
                else -> throw IllegalArgumentException("Wrong task state.")
            }

            else -> throw IllegalArgumentException("Wrong DataItem child.")
        }
    }

    interface SearchableVH {

        fun bindSearchable(item: Searchable)

    }

    class SearchItemDiffUtilCallback : DiffUtil.ItemCallback<Searchable>() {

        override fun areItemsTheSame(oldItem: Searchable, newItem: Searchable): Boolean {
            return when {
                oldItem is NoteElement && newItem is NoteElement -> oldItem.id == newItem.id
                oldItem is TaskElement && newItem is TaskElement -> oldItem.id == newItem.id
                else -> false
            }
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: Searchable, newItem: Searchable): Boolean {
            return when {
                oldItem is NoteElement && newItem is NoteElement -> oldItem == newItem
                oldItem is TaskElement && newItem is TaskElement -> oldItem == newItem
                else -> false
            }
        }
    }

    interface Searchable

    companion object {

        const val VIEW_TYPE_NOTE = 0
        const val VIEW_TYPE_TASK_ACTIVE = 1
        const val VIEW_TYPE_TASK_FINISHED = 2

    }

}