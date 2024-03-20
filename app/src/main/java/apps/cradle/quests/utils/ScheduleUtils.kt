package apps.cradle.quests.utils

import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbAction
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.DateElement
import apps.cradle.quests.models.EmptyElement
import apps.cradle.quests.models.ScheduleSummaryElement
import apps.cradle.quests.models.SpaceElement
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.models.TodayEmptyElement
import apps.cradle.quests.ui.adapters.TaskItemsAdapter
import java.util.Date

object ScheduleUtils {

    fun getTaskItemsForMainScreen(): List<TaskItem> {
        val tasks = App.db.tasksDao().getAllActiveTasks()
            .sortedWith(
                compareBy(
                    { -it.date },
                    { it.time != DbTask.NO_TIME },
                    { it.deadline != DbTask.NO_DEADLINE },
                    { -it.time },
                    { -it.deadline },
                )
            )
        val result = mutableListOf<TaskItem>()
        var currentDate = Long.MAX_VALUE
        for (task in tasks) {
            val taskDate = resetTimeInMillis(task.date)
            if (currentDate != taskDate) {
                currentDate = taskDate
                result.add(DateElement(date = currentDate))
            }
            result.add(createTaskElement(task, getQuestsMap(), getTasksActionsMap()))
        }
        return fillTaskItemsWithEmptyDatesAndSummary(result)
    }

    fun createTaskElement(
        task: DbTask,
        quests: Map<String, String>,
        tasksActionsMap: Map<String, List<DbAction>>
    ): TaskElement {
        return TaskElement(
            id = task.id,
            questId = task.questId,
            questTitle = quests[task.questId].orEmpty(),
            title = task.title,
            date = task.date,
            time = task.time,
            reminder = task.reminder,
            state = task.state,
            actionsCount = tasksActionsMap[task.id]?.size ?: 0,
            doneActionsCount = tasksActionsMap[task.id]?.count { it.state == DbAction.STATE_FINISHED }
                ?: 0,
            deadline = task.deadline
        )
    }

    fun getQuestsMap(): Map<String, String> {
        val map = App.db.questsDao().getAll().associate { it.id to it.title }.toMutableMap()
        map[DbQuest.HEAP_QUEST_ID] = App.instance.getString(R.string.heapQuestTitle)
        return map
    }

    fun getTasksActionsMap(): Map<String, List<DbAction>> {
        val allActions = App.db.actionsDao().getAllActions()
        val tasksIds = allActions.distinctBy { it.taskId }.map { it.taskId }
        val result = mutableMapOf<String, List<DbAction>>()
        for (taskId in tasksIds) result[taskId] = allActions.filter { it.taskId == taskId }
        return result
    }

    private fun getScheduleSummary(taskItems: List<TaskItem>): ScheduleSummaryElement {
        val today = resetTimeInMillis(Date().time)
        val staleTasksCount = taskItems.filterIsInstance(TaskElement::class.java)
            .count { resetTimeInMillis(it.date) < today }
        val todayTasksCount = taskItems.filterIsInstance(TaskElement::class.java)
            .count { resetTimeInMillis(it.date) == today }
        val futureTasksCount = taskItems.filterIsInstance(TaskElement::class.java)
            .count { resetTimeInMillis(it.date) > today }
        return ScheduleSummaryElement(
            staleTasksCount = staleTasksCount,
            todayTasksCount = todayTasksCount,
            futureTasksCount = futureTasksCount
        )
    }

    private fun fillTaskItemsWithEmptyDatesAndSummary(taskItems: List<TaskItem>): List<TaskItem> {
        val today = resetTimeInMillis(System.currentTimeMillis())
        val staleTasks = taskItems.filter { resetTimeInMillis(it.itemDate) < today }
        val lastDate = resetTimeInMillis(taskItems.maxOfOrNull { it.itemDate } ?: today)
        val result = mutableListOf<TaskItem>()
        for (date in lastDate downTo today step TaskItemsAdapter.DAY_LENGTH_MILLIS) {
            val tasksForADate = taskItems.filter { resetTimeInMillis(it.itemDate) == date }
            if (date == today) result.add(SpaceElement())
            if (tasksForADate.isNotEmpty()) {
                result.addAll(tasksForADate)
            } else {
                if (date == today && staleTasks.isEmpty()) result.add(TodayEmptyElement(date))
                else result.add(EmptyElement(date))
            }
        }
        result.addAll(staleTasks)
        result.add(getScheduleSummary(taskItems))
        return result
    }

}