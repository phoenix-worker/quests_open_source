package apps.cradle.quests.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actions")
data class DbAction(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "state")
    val state: Int
) {

    companion object {
        const val STATE_ACTIVE = 1
        const val STATE_FINISHED = 2
    }

}