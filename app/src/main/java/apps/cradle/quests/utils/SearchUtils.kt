package apps.cradle.quests.utils

import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.models.toNoteElements
import apps.cradle.quests.ui.adapters.SearchItemsAdapter.Searchable

fun totalSearch(searchClause: String): List<Searchable> {
    val result = mutableListOf<Searchable>()
    val notes = App.db.notesDao().searchNotes(searchClause).toNoteElements()
        .sortedBy { -it.created }
    val tasks = App.db.tasksDao().searchTasks(searchClause).toTaskElements()
        .sortedBy { -it.date }
    result.addAll(notes)
    result.addAll(tasks)
    return result
}

private fun List<DbTask>.toTaskElements(): List<TaskElement> {
    val questsMap = ScheduleUtils.getQuestsMap()
    val actionsMap = ScheduleUtils.getTasksActionsMap()
    return this.map { dbTask ->
        ScheduleUtils.createTaskElement(dbTask, questsMap, actionsMap)
    }
}