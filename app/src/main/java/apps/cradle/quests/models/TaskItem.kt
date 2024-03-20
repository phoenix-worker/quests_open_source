package apps.cradle.quests.models

import apps.cradle.quests.ui.adapters.SearchItemsAdapter

sealed class TaskItem(val itemDate: Long)

data class TaskElement(
    val id: String,
    val questId: String,
    val questTitle: String,
    val title: String,
    val date: Long,
    val time: Long,
    val reminder: Long,
    val state: Int,
    val actionsCount: Int,
    val doneActionsCount: Int,
    val deadline: Long
) : TaskItem(date), SearchItemsAdapter.Searchable

data class DateElement(
    val date: Long
) : TaskItem(date)

data class EmptyElement(
    val date: Long
) : TaskItem(date)

data class TodayEmptyElement(
    val date: Long
) : TaskItem(date)

data class DividerElement(
    var isExpanded: Boolean,
    val finishedTasksCount: Int
) : TaskItem(0L)

data class ScheduleSummaryElement(
    val staleTasksCount: Int,
    val todayTasksCount: Int,
    val futureTasksCount: Int
) : TaskItem(0L)

class SpaceElement : TaskItem(0L)

class FinishElement(
    val questId: String
) : TaskItem(0L)