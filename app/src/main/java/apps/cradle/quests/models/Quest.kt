package apps.cradle.quests.models

import apps.cradle.quests.database.entities.DbQuest

data class Quest(
    val id: String,
    val categoryId: String,
    val title: String
)

fun DbQuest.toQuest(): Quest {
    return Quest(
        id = id,
        categoryId = categoryId,
        title = title
    )
}