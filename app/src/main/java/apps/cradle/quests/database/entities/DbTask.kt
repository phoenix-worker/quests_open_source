package apps.cradle.quests.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class DbTask(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "quest_id")
    val questId: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "date")
    val date: Long,
    @ColumnInfo(name = "time")
    val time: Long,
    @ColumnInfo(name = "reminder")
    val reminder: Long,
    @ColumnInfo(name = "state")
    val state: Int,
    @ColumnInfo(name = "deadline")
    val deadline: Long
) {

    @Suppress("unused")
    companion object {

        const val STATE_ACTIVE = 1
        const val STATE_FINISHED = 2

        const val NO_TIME = -1L
        const val NO_DEADLINE = 0L

        const val REMINDER_DEFAULT = 0L
        const val REMINDER_5_MIN = 5L * 60L * 1000L
        const val REMINDER_10_MIN = 10L * 60L * 1000L
        const val REMINDER_15_MIN = 15L * 60L * 1000L
        const val REMINDER_30_MIN = 30L * 60L * 1000L
        const val REMINDER_1_HOUR = 60L * 60L * 1000L
        const val REMINDER_2_HOURS = 2L * 60L * 60L * 1000L

    }

}