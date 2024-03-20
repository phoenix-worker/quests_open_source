package apps.cradle.quests.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class DbCategory(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "title")
    val title: String
) {

    companion object {

        const val WITHOUT_CATEGORY = "without_category"
        const val ARCHIVE_CATEGORY = "archive_category"

    }

}