package apps.cradle.quests.models

import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.utils.QuestsUtils

sealed class QuestItem

@Suppress("unused")
data class QuestElement(
    val id: String,
    val categoryId: String,
    val title: String,
    val state: QuestState,
    val activeTasksCount: Int,
    val finishedTasksCount: Int,
    val notesCount: Int
) : QuestItem()

enum class QuestState { ACTIVE, DATA, EMPTY, FINISHED }

fun List<DbQuest>.toQuestsElements(): List<QuestElement> {
    return map { dbQuest ->
        val tasks = App.db.tasksDao().getAllQuestTasks(dbQuest.id)
        val activeTasks = tasks.filter { it.state == DbTask.STATE_ACTIVE }
        val data = QuestsUtils.getQuestDataItems(dbQuest.id)
        QuestElement(
            id = dbQuest.id,
            categoryId = dbQuest.categoryId,
            title = dbQuest.title,
            state = when {
                tasks.isNotEmpty() -> {
                    if (activeTasks.isNotEmpty() || dbQuest.id == DbQuest.HEAP_QUEST_ID) QuestState.ACTIVE
                    else QuestState.FINISHED
                }

                data.isNotEmpty() -> QuestState.DATA
                else -> QuestState.EMPTY
            },
            activeTasksCount = tasks.count { it.state == DbTask.STATE_ACTIVE },
            finishedTasksCount = tasks.count { it.state == DbTask.STATE_FINISHED },
            notesCount = data.filterIsInstance<NoteElement>().size
        )
    }
}