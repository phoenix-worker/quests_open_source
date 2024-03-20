package apps.cradle.quests.models

import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.database.entities.DbNote
import apps.cradle.quests.ui.adapters.SearchItemsAdapter
import apps.cradle.quests.utils.ScheduleUtils

sealed class DataItem(val creationDate: Long)

data class NoteElement(
    val id: String,
    val questId: String,
    val questTitle: String,
    val title: String,
    val content: String,
    val created: Long,
    val isArchived: Boolean
) : DataItem(created), SearchItemsAdapter.Searchable

fun List<DbNote>.toNoteElements(): List<NoteElement> {
    val questMap = ScheduleUtils.getQuestsMap()
    return map {
        val quest = App.db.questsDao().getQuest(it.questId)
        NoteElement(
            id = it.id,
            questId = it.questId,
            questTitle = questMap[it.questId].orEmpty(),
            title = it.title,
            content = it.content,
            created = it.created,
            isArchived = quest?.categoryId == DbCategory.ARCHIVE_CATEGORY
        )
    }
}

fun DbNote.toNoteElement(): NoteElement {
    val quest = App.db.questsDao().getQuest(questId)
    return NoteElement(
        id = id,
        questId = questId,
        questTitle = quest?.title.orEmpty(),
        title = title,
        content = content,
        created = created,
        isArchived = quest?.categoryId == DbCategory.ARCHIVE_CATEGORY
    )
}