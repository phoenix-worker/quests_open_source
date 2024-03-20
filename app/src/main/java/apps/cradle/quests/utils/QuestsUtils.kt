package apps.cradle.quests.utils

import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.DataItem
import apps.cradle.quests.models.DividerElement
import apps.cradle.quests.models.FinishElement
import apps.cradle.quests.models.QuestItem
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.models.toNoteElements
import apps.cradle.quests.models.toQuestsElements

object QuestsUtils {

    fun getQuestItems(categoryId: String): List<QuestItem> {
        val quests = App.db.questsDao().getQuestsInCategory(categoryId).toQuestsElements()
        val result = mutableListOf<QuestItem>()
        if (quests.isNotEmpty()) {
            result.addAll(quests.sortedBy { it.state })
        }
        return result
    }

    fun getQuestTaskItems(questId: String, includeFinished: Boolean): List<TaskItem> {
        val allTasks = App.db.tasksDao().getAllQuestTasks(questId).sortedBy { it.date }
        val activeTasks = allTasks.filter { it.state == DbTask.STATE_ACTIVE }
        val finishedTasks =
            allTasks.filter { it.state == DbTask.STATE_FINISHED }.sortedBy { -it.date }
        val quests = App.db.questsDao().getAll().associate { it.id to it.title }
        val result = mutableListOf<TaskItem>()
        val taskActionsMap = ScheduleUtils.getTasksActionsMap()
        result.addAll(activeTasks.map {
            ScheduleUtils.createTaskElement(
                it,
                quests,
                taskActionsMap
            )
        })
        if (includeFinished) result.addAll(finishedTasks.map { dbTask ->
            ScheduleUtils.createTaskElement(dbTask, quests, taskActionsMap)
        })
        val isHeapQuest = questId == DbQuest.HEAP_QUEST_ID
        if (allTasks.isNotEmpty() && activeTasks.isEmpty() && !isHeapQuest)
            result.add(0, FinishElement(questId))
        return insertTasksDivider(result, finishedTasks.size, includeFinished)
    }

    fun getQuestTaskItemsForArchivedQuest(questId: String): List<TaskItem> {
        val allTasks = App.db.tasksDao().getAllQuestTasks(questId).sortedBy { it.date }
        val activeTasks = allTasks.filter { it.state == DbTask.STATE_ACTIVE }
        val finishedTasks =
            allTasks.filter { it.state == DbTask.STATE_FINISHED }.sortedBy { -it.date }
        val quests = App.db.questsDao().getAll().associate { it.id to it.title }
        val result = mutableListOf<TaskItem>()
        val taskActionsMap = ScheduleUtils.getTasksActionsMap()
        result.addAll(activeTasks.map {
            ScheduleUtils.createTaskElement(
                it,
                quests,
                taskActionsMap
            )
        })
        result.addAll(finishedTasks.map {
            ScheduleUtils.createTaskElement(
                it,
                quests,
                taskActionsMap
            )
        })
        return result
    }

    private fun insertTasksDivider(
        items: MutableList<TaskItem>,
        finishedTasksCount: Int,
        includeFinished: Boolean
    ): List<TaskItem> {
        return if (finishedTasksCount > 0) {
            val firstFinishedIndex = items.indexOfFirst {
                it is TaskElement && it.state == DbTask.STATE_FINISHED
            }
            val insertIndex = if (firstFinishedIndex >= 0) firstFinishedIndex else items.size
            items.add(
                insertIndex,
                DividerElement(includeFinished, finishedTasksCount)
            )
            return items
        } else items
    }

    fun deleteQuestAndAllItsData(questId: String) {
        val questTasks = App.db.tasksDao().getAllQuestTasks(questId)
        for (task in questTasks) App.db.actionsDao().deleteAllTaskActions(task.id)
        App.db.tasksDao().deleteAllQuestTasks(questId)
        deleteQuestMaterials(questId)
        App.db.questsDao().deleteQuestById(questId)
        Locator.sendConditionsChangedEvent()
    }

    private fun deleteQuestMaterials(questId: String) {
        App.db.notesDao().deleteAllQuestNotes(questId)
    }

    fun getQuestDataItems(questId: String): List<DataItem> {
        val result = mutableListOf<DataItem>()
        result.addAll(App.db.notesDao().getQuestNotes(questId).toNoteElements())
        return result.sortedBy { -it.creationDate }
    }

    fun archiveQuest(questId: String) {
        App.db.questsDao().setQuestCategory(questId, DbCategory.ARCHIVE_CATEGORY)
        Locator.sendConditionsChangedEvent()
    }

}