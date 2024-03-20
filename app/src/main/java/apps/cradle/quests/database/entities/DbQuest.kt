package apps.cradle.quests.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class DbQuest(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "title")
    val title: String
) {

    companion object {

        const val HEAP_QUEST_ID = "heap"

    }

}