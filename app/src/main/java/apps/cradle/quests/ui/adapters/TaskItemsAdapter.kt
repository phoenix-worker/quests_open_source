package apps.cradle.quests.ui.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.databinding.RvDateElementBinding
import apps.cradle.quests.databinding.RvEmptyDateBinding
import apps.cradle.quests.databinding.RvFinishedQuestHintBinding
import apps.cradle.quests.databinding.RvScheduleSummaryBinding
import apps.cradle.quests.databinding.RvSpaceBinding
import apps.cradle.quests.databinding.RvTaskElementBinding
import apps.cradle.quests.databinding.RvTaskElementFinishedBinding
import apps.cradle.quests.databinding.RvTasksDividerBinding
import apps.cradle.quests.databinding.RvTodayEmptyBinding
import apps.cradle.quests.models.DateElement
import apps.cradle.quests.models.DividerElement
import apps.cradle.quests.models.EmptyElement
import apps.cradle.quests.models.FinishElement
import apps.cradle.quests.models.ScheduleSummaryElement
import apps.cradle.quests.models.SpaceElement
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.models.TodayEmptyElement
import apps.cradle.quests.ui.fragments.schedule.ScheduleViewModel
import apps.cradle.quests.ui.fragments.schedule.ScheduleViewModel.MenuItemEvent.DATABASE
import apps.cradle.quests.ui.fragments.schedule.ScheduleViewModel.MenuItemEvent.QUICK_NOTE
import apps.cradle.quests.ui.fragments.schedule.ScheduleViewModel.MenuItemEvent.QUICK_TASK
import apps.cradle.quests.ui.views.ActionView
import apps.cradle.quests.utils.Locator
import apps.cradle.quests.utils.formatTimeFromMillis
import apps.cradle.quests.utils.fullDateFormat
import apps.cradle.quests.utils.resetTimeInMillis
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskItemsAdapter(
    private val mode: MODE,
    private val onTaskClicked: (String) -> Unit,
    private val onFinishedTaskClicked: (String) -> Unit,
    private val onTaskLongClicked: (String, String) -> Unit,
    private val onDividerClicked: (() -> Unit)? = null,
    private val onArchiveClicked: ((String) -> Unit)? = null,
    private val scheduleVM: ScheduleViewModel? = null
) : ListAdapter<TaskItem, TaskItemsAdapter.TaskItemVH>(TaskItemDiffUtilCallback()) {

    private var initTime = resetTimeInMillis(System.currentTimeMillis())

    @SuppressLint("NotifyDataSetChanged")
    fun checkIsTodayChanged() {
        if (resetTimeInMillis(System.currentTimeMillis()) != initTime) {
            initTime = resetTimeInMillis(System.currentTimeMillis())
            notifyDataSetChanged()
        }
    }

    var isMenuExpanded: Boolean = false
        private set

    fun setIsExpanded(isMenuExpanded: Boolean) {
        this.isMenuExpanded = isMenuExpanded
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskItemVH {
        return when (viewType) {
            VIEW_TYPE_TASK_ELEMENT_ACTIVE -> TaskElementVH.getViewHolder(
                parent,
                mode,
                onTaskClicked,
                onTaskLongClicked
            )

            VIEW_TYPE_TASK_ELEMENT_FINISHED -> FinishedTaskElementVH.getViewHolder(
                parent,
                onFinishedTaskClicked
            )

            VIEW_TYPE_DATE_ELEMENT -> DateElementVH.getViewHolder(parent)
            VIEW_TYPE_EMPTY_ELEMENT -> EmptyElementVH.getViewHolder(parent)
            VIEW_TYPE_DIVIDER_ELEMENT -> {
                if (onDividerClicked == null) throw IllegalArgumentException("Divider must have onClick()")
                TasksDividerVH.getViewHolder(parent, onDividerClicked)
            }

            VIEW_TYPE_TODAY_EMPTY -> TodayEmptyVH.getViewHolder(parent)
            VIEW_TYPE_SCHEDULE_SUMMARY -> ScheduleSummaryVH.getViewHolder(parent, scheduleVM, this)
            VIEW_TYPE_SPACE -> SpaceVH.getViewHolder(parent)
            VIEW_TYPE_FINISH -> FinishVH.getViewHolder(parent, onArchiveClicked)
            else -> throw IllegalArgumentException("Wrong TaskItem view type.")
        }
    }

    override fun onBindViewHolder(holder: TaskItemVH, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is TaskElement -> when (item.state) {
                DbTask.STATE_ACTIVE -> VIEW_TYPE_TASK_ELEMENT_ACTIVE
                DbTask.STATE_FINISHED -> VIEW_TYPE_TASK_ELEMENT_FINISHED
                else -> throw IllegalArgumentException("Wrong task state.")
            }

            is DateElement -> VIEW_TYPE_DATE_ELEMENT
            is EmptyElement -> VIEW_TYPE_EMPTY_ELEMENT
            is DividerElement -> VIEW_TYPE_DIVIDER_ELEMENT
            is TodayEmptyElement -> VIEW_TYPE_TODAY_EMPTY
            is ScheduleSummaryElement -> VIEW_TYPE_SCHEDULE_SUMMARY
            is SpaceElement -> VIEW_TYPE_SPACE
            is FinishElement -> VIEW_TYPE_FINISH
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<TaskItem>,
        currentList: MutableList<TaskItem>
    ) {
        scheduleVM?.sendScrollToBottomEventIfRequired()
        checkIsTodayChanged()
    }

    sealed class TaskItemVH(view: View) : RecyclerView.ViewHolder(view) {

        abstract fun bind(taskItem: TaskItem)

    }

    class ScheduleSummaryVH(
        private val binding: RvScheduleSummaryBinding,
        private val scheduleVM: ScheduleViewModel?,
        private val adapter: TaskItemsAdapter
    ) : TaskItemVH(binding.root) {

        override fun bind(taskItem: TaskItem) {
            (taskItem as ScheduleSummaryElement).let { summary ->
                val context = itemView.context
                binding.staleTasks.text = getStaleTasksText(context, summary.staleTasksCount)
                binding.todayTasks.text =
                    getTodayTasksText(context, summary.todayTasksCount, R.plurals.todayTasksCount)
                binding.futureTasks.text = getFutureTasksText(context, summary.futureTasksCount)
                binding.staleTasks.isVisible = summary.staleTasksCount > 0
                binding.futureTasks.isVisible = summary.futureTasksCount > 0
                when (summary.todayTasksCount) {
                    0 -> {
                        binding.todayTasks.setBackgroundResource(R.drawable.background_rounded_rect_green)
                        val padding = context.resources.getDimension(R.dimen.smallMargin).toInt()
                        binding.todayTasks.setPadding(padding, 0, padding, 0)
                    }

                    else -> {
                        binding.todayTasks.setBackgroundResource(0)
                        binding.todayTasks.setPadding(0, 0, 0, 0)
                    }
                }
                setListeners()
                setExpandedState()
            }
        }

        private fun setExpandedState() {
            when {
                adapter.isMenuExpanded -> {
                    binding.expand.isVisible = false
                    binding.container.isVisible = true
                }

                else -> {
                    binding.container.isVisible = false
                    binding.expand.isVisible = true
                }
            }
        }

        private fun toggleExpandedState() {
            adapter.setIsExpanded(!adapter.isMenuExpanded)
            setExpandedState()
            adapter.notifyItemChanged(adapter.itemCount - 1)
            if (adapter.isMenuExpanded) scheduleVM?.sendScrollToBottomEvent()
        }

        private fun setListeners() {
            binding.quickTask.setOnClickListener { scheduleVM?.sendMenuItemEvent(QUICK_TASK) }
            binding.quickNote.setOnClickListener { scheduleVM?.sendMenuItemEvent(QUICK_NOTE) }
            binding.database.setOnClickListener { scheduleVM?.sendMenuItemEvent(DATABASE) }
            binding.expand.setOnClickListener { toggleExpandedState() }
            binding.collapse.setOnClickListener { toggleExpandedState() }
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                scheduleVM: ScheduleViewModel?,
                adapter: TaskItemsAdapter
            ): ScheduleSummaryVH {
                val binding = RvScheduleSummaryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return ScheduleSummaryVH(binding, scheduleVM, adapter)
            }

            fun getStaleTasksText(context: Context, staleTasksCount: Int): String {
                return when (staleTasksCount) {
                    0 -> context.resources.getString(R.string.noStaleTasks)
                    else -> context.resources.getQuantityString(
                        R.plurals.staleTasksCount,
                        staleTasksCount,
                        staleTasksCount.toString()
                    )
                }
            }

            fun getTodayTasksText(
                context: Context,
                todayTasksCount: Int,
                resId: Int
            ): String {
                return when (todayTasksCount) {
                    0 -> context.resources.getString(R.string.noTodayTasks)
                    else -> context.resources.getQuantityString(
                        resId,
                        todayTasksCount,
                        todayTasksCount.toString()
                    )
                }
            }

            fun getFutureTasksText(context: Context, futureTasksCount: Int): String {
                return when (futureTasksCount) {
                    0 -> context.resources.getString(R.string.noFutureTasks)
                    else -> context.resources.getQuantityString(
                        R.plurals.futureTasksCount,
                        futureTasksCount,
                        futureTasksCount.toString()
                    )
                }
            }

        }

    }

    class FinishVH(
        private val binding: RvFinishedQuestHintBinding,
        private val onArchiveClicked: ((String) -> Unit)?
    ) : TaskItemVH(binding.root) {

        override fun bind(taskItem: TaskItem) {
            (taskItem as FinishElement).questId.let { questId ->
                binding.image.setOnClickListener {
                    onArchiveClicked?.invoke(questId)
                }
            }
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                onArchiveClicked: ((String) -> Unit)?
            ): FinishVH {
                val binding = RvFinishedQuestHintBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FinishVH(binding, onArchiveClicked)
            }

        }

    }

    class TasksDividerVH(
        private val binding: RvTasksDividerBinding,
        private val onClick: () -> Unit
    ) : TaskItemVH(binding.root) {

        override fun bind(taskItem: TaskItem) {
            (taskItem as DividerElement).let { divider ->
                binding.action.setState(
                    when {
                        divider.isExpanded -> ActionView.STATE.SHOWN
                        else -> ActionView.STATE.HIDDEN
                    }
                )
                val count = divider.finishedTasksCount
                binding.action.setTitle(
                    App.instance.resources.getQuantityString(
                        R.plurals.finishedTasksCount,
                        count,
                        count
                    )
                )
                binding.action.setOnClickListener {
                    binding.action.switch(onClick)
                }
            }
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                onClick: () -> Unit
            ): TasksDividerVH {
                val binding = RvTasksDividerBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TasksDividerVH(binding, onClick)
            }

        }

    }

    class TodayEmptyVH(
        binding: RvTodayEmptyBinding
    ) : TaskItemVH(binding.root) {

        override fun bind(taskItem: TaskItem) {}

        companion object {

            fun getViewHolder(parent: ViewGroup): TodayEmptyVH {
                val binding = RvTodayEmptyBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TodayEmptyVH(binding)
            }

        }

    }

    class SpaceVH(
        binding: RvSpaceBinding
    ) : TaskItemVH(binding.root) {

        override fun bind(taskItem: TaskItem) {}

        companion object {

            fun getViewHolder(parent: ViewGroup): SpaceVH {
                val binding = RvSpaceBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return SpaceVH(binding)
            }

        }

    }

    class TaskElementVH(
        private val binding: RvTaskElementBinding,
        private val mode: MODE,
        private val onClick: (String) -> Unit,
        private val onLongClick: (String, String) -> Unit
    ) : TaskItemVH(binding.root), SearchItemsAdapter.SearchableVH {

        private val dateFormat = SimpleDateFormat(fullDateFormat, Locale.getDefault())

        override fun bindSearchable(item: SearchItemsAdapter.Searchable) {
            bind(item as TaskItem)
        }

        @SuppressLint("SetTextI18n")
        override fun bind(taskItem: TaskItem) {
            (taskItem as TaskElement).let { task ->
                binding.title.text = task.title
                binding.questInfo.text = task.questTitle
                binding.questInfo.isVisible = mode != MODE.QUEST
                binding.questInfo.setOnClickListener { Locator.sendOpenQuestEvent(task.questId) }
                binding.root.setOnClickListener { onClick.invoke(task.id) }
                binding.root.setOnLongClickListener {
                    onLongClick.invoke(task.questId, task.id)
                    true
                }
                if (task.actionsCount == 0) binding.actions.visibility = View.GONE
                else {
                    binding.actions.text = "${task.doneActionsCount}/${task.actionsCount}"
                    binding.actions.visibility = View.VISIBLE
                }
                setDeadline(task)
                setTime(task)
                setBackground(task)
            }
        }

        private fun setBackground(task: TaskElement) {
            val today = resetTimeInMillis(System.currentTimeMillis())
            val taskDate = resetTimeInMillis(task.date)
            binding.root.setBackgroundResource(
                when {
                    mode == MODE.QUEST -> R.drawable.background_task_item
                    taskDate < today -> R.drawable.background_task_item_stale
                    else -> R.drawable.background_task_item
                }
            )
        }

        private fun setTime(task: TaskElement) {
            if (mode == MODE.SEARCH || mode == MODE.QUEST) {
                binding.date.text = dateFormat.format(Date(task.date))
                binding.date.isVisible = true
            } else {
                binding.date.isVisible = false
            }
            if (task.time == DbTask.NO_TIME) {
                binding.time.visibility = View.GONE
                binding.notification.visibility = View.GONE
            } else {
                binding.time.text = formatTimeFromMillis(task.time)
                binding.notification.text = getNotificationText(task)
                binding.time.visibility = View.VISIBLE
                binding.notification.visibility = View.VISIBLE
            }
        }

        private fun getNotificationText(task: TaskElement): String {
            val context = itemView.context
            return when (task.reminder) {
                DbTask.REMINDER_DEFAULT -> context.getString(R.string.chipReminderDefaultShort)
                DbTask.REMINDER_5_MIN -> context.getString(R.string.chipReminder5Min)
                DbTask.REMINDER_10_MIN -> context.getString(R.string.chipReminder10Min)
                DbTask.REMINDER_15_MIN -> context.getString(R.string.chipReminder15Min)
                DbTask.REMINDER_30_MIN -> context.getString(R.string.chipReminder30Min)
                DbTask.REMINDER_1_HOUR -> context.getString(R.string.chipReminder1Hour)
                DbTask.REMINDER_2_HOURS -> context.getString(R.string.chipReminder2Hours)
                else -> throw IllegalArgumentException("Wrong reminder value.")
            }
        }

        private fun setDeadline(task: TaskElement) {
            if (task.deadline == 0L || task.state == DbTask.STATE_FINISHED)
                binding.deadline.visibility = View.GONE
            else {
                val deadline = resetTimeInMillis(task.deadline)
                val taskDate = resetTimeInMillis(task.date)
                when {
                    deadline < taskDate -> {
                        binding.deadline.text = App.instance.getString(R.string.wasted)
                        binding.deadline.visibility = View.VISIBLE
                    }

                    deadline == taskDate -> {
                        binding.deadline.text = App.instance.getString(R.string.deadlineToday)
                        binding.deadline.visibility = View.VISIBLE
                    }

                    else -> {
                        val daysDiff = ((deadline - taskDate) / DAY_LENGTH_MILLIS).toInt()
                        val text = App.instance.resources.getQuantityString(
                            R.plurals.daysBeforeDeadline,
                            daysDiff,
                            daysDiff.toString()
                        )
                        binding.deadline.text = text
                        binding.deadline.visibility = View.VISIBLE
                    }
                }
                binding.deadline.visibility = View.VISIBLE
            }
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                mode: MODE,
                onClick: (String) -> Unit,
                onLongClick: (String, String) -> Unit
            ): TaskElementVH {
                val binding = RvTaskElementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TaskElementVH(binding, mode, onClick, onLongClick)
            }

        }

    }

    class FinishedTaskElementVH(
        private val binding: RvTaskElementFinishedBinding,
        private val onFinishedTaskClicked: (String) -> Unit
    ) : TaskItemVH(binding.root), SearchItemsAdapter.SearchableVH {

        override fun bind(taskItem: TaskItem) {
            (taskItem as TaskElement).let { task ->
                binding.apply {
                    date.text = getTaskDateString(task)
                    title.text = when {
                        task.actionsCount > 0 -> App.instance.resources.getString(
                            R.string.finishedTaskTitleWithActions,
                            task.title,
                            task.doneActionsCount.toString(),
                            task.actionsCount.toString()
                        )

                        else -> task.title
                    }
                    questInfo.text = task.questTitle
                    questInfo.isVisible = false
                    questInfo.setOnClickListener { Locator.sendOpenQuestEvent(task.questId) }
                    root.setOnClickListener { onFinishedTaskClicked.invoke(task.id) }
                }
            }
        }

        override fun bindSearchable(item: SearchItemsAdapter.Searchable) {
            bind(item as TaskItem)
            binding.questInfo.isVisible = true
        }

        companion object {

            fun getViewHolder(
                parent: ViewGroup,
                onFinishedTaskClicked: (String) -> Unit
            ): FinishedTaskElementVH {
                val binding = RvTaskElementFinishedBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FinishedTaskElementVH(binding, onFinishedTaskClicked)
            }

            fun getTaskDateString(task: TaskElement): String {
                val dateFormat = SimpleDateFormat(fullDateFormat, Locale.getDefault())
                val dateText = dateFormat.format(Date(task.date))
                return when (task.time) {
                    DbTask.NO_TIME -> dateText
                    else -> App.instance.resources.getString(
                        R.string.finishedTaskDate,
                        dateText,
                        formatTimeFromMillis(task.time)
                    )
                }
            }
        }
    }

    class DateElementVH(
        private val binding: RvDateElementBinding
    ) : TaskItemVH(binding.root) {

        private val dateFormat = SimpleDateFormat(fullDateFormat, Locale.getDefault())
        private val dayFormat = SimpleDateFormat("E", Locale.getDefault())
        private val calendar = Calendar.getInstance(Locale.getDefault())

        @SuppressLint("SetTextI18n")
        override fun bind(taskItem: TaskItem) {
            setDate((taskItem as DateElement))
        }

        private fun setDate(dateElement: DateElement) {
            val currentDate = resetTimeInMillis(System.currentTimeMillis())
            val taskDate = resetTimeInMillis(dateElement.date)
            val daysDiff = taskDate - currentDate
            val dateString = dateFormat.format(Date(dateElement.date))
            binding.date.run {
                when (daysDiff) {
                    -(2 * DAY_LENGTH_MILLIS) ->
                        text = getDateString(R.string.dayBeforeYesterday, dateString)

                    -DAY_LENGTH_MILLIS ->
                        text = getDateString(R.string.yesterday, dateString)

                    0L ->
                        text = getDateString(R.string.today, dateString)

                    DAY_LENGTH_MILLIS ->
                        text = getDateString(R.string.tomorrow, dateString)

                    2 * DAY_LENGTH_MILLIS ->
                        text = getDateString(R.string.dayAfterTomorrow, dateString)

                    else -> binding.date.text = dateString
                }
            }
            calendar.timeInMillis = dateElement.itemDate
            binding.day.setBackgroundResource(
                when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SATURDAY, Calendar.SUNDAY -> R.drawable.background_empty_date_holiday
                    else -> R.drawable.background_empty_date
                }
            )
            binding.day.text = dayFormat.format(Date(dateElement.itemDate))
            binding.day.visibility = View.VISIBLE
        }

        private fun getDateString(res: Int, dateString: String): String {
            return "${App.instance.getString(res)}, $dateString"
        }

        companion object {

            fun getViewHolder(parent: ViewGroup): DateElementVH {
                val binding = RvDateElementBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return DateElementVH(binding)
            }

        }

    }

    class EmptyElementVH(
        private val binding: RvEmptyDateBinding
    ) : TaskItemVH(binding.root) {

        private val calendar = Calendar.getInstance(Locale.getDefault())
        private val dayFormat = SimpleDateFormat("E", Locale.getDefault())

        override fun bind(taskItem: TaskItem) {
            calendar.timeInMillis = taskItem.itemDate
            binding.day.setBackgroundResource(
                when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.SATURDAY, Calendar.SUNDAY -> R.drawable.background_empty_date_holiday
                    else -> R.drawable.background_empty_date
                }
            )
            binding.day.text = dayFormat.format(Date(taskItem.itemDate))
        }

        companion object {

            fun getViewHolder(parent: ViewGroup): EmptyElementVH {
                val binding = RvEmptyDateBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return EmptyElementVH(binding)
            }

        }

    }

    class TaskItemDiffUtilCallback : DiffUtil.ItemCallback<TaskItem>() {

        override fun areItemsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean {
            if (oldItem is ScheduleSummaryElement && newItem is ScheduleSummaryElement) return true
            if (oldItem is DividerElement && newItem is DividerElement) return true
            if (oldItem is FinishElement && newItem is FinishElement) return true
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: TaskItem, newItem: TaskItem): Boolean {
            if (oldItem is FinishElement && newItem is FinishElement) return true
            if (oldItem is DividerElement && newItem is DividerElement) return true
            return oldItem == newItem
        }

    }

    enum class MODE { SCHEDULE, QUEST, SEARCH }

    companion object {

        const val VIEW_TYPE_TASK_ELEMENT_ACTIVE = 0
        const val VIEW_TYPE_TASK_ELEMENT_FINISHED = 1
        const val VIEW_TYPE_DATE_ELEMENT = 2
        const val VIEW_TYPE_EMPTY_ELEMENT = 3
        const val VIEW_TYPE_DIVIDER_ELEMENT = 4
        const val VIEW_TYPE_TODAY_EMPTY = 5
        const val VIEW_TYPE_SCHEDULE_SUMMARY = 6
        const val VIEW_TYPE_SPACE = 7
        const val VIEW_TYPE_FINISH = 8
        const val DAY_LENGTH_MILLIS = 24L * 60L * 60L * 1000L

    }

}