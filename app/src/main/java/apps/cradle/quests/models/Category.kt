package apps.cradle.quests.models

import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.utils.UniqueIdGenerator

data class Category(
    val id: String,
    val title: String,
    val viewPageId: Long
)

fun List<DbCategory>.toCategories(): List<Category> {
    return map {
        Category(
            id = it.id,
            title = it.title,
            viewPageId = UniqueIdGenerator.nextId()
        )
    }
}