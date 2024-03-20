package apps.cradle.quests.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.cradle.quests.database.entities.DbAction

@Dao
interface ActionsDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(dbAction: DbAction)

    @Query("DELETE FROM actions WHERE id = :actionId")
    fun deleteActionById(actionId: String): Int

    @Query("SELECT * FROM actions WHERE id = :actionId")
    fun getAction(actionId: String): DbAction?

    @Query("SELECT * FROM actions WHERE task_id = :taskId")
    fun getAllTaskActions(taskId: String): List<DbAction>

    @Query("UPDATE actions SET title = :title WHERE id = :actionId")
    fun setActionTitle(actionId: String, title: String)

    @Query("UPDATE actions SET state = :state WHERE id = :actionId")
    fun setActionState(actionId: String, state: Int)

    @Query("DELETE FROM actions WHERE task_id = :taskId")
    fun deleteAllTaskActions(taskId: String)

    @Query("SELECT * FROM actions")
    fun getAllActions(): List<DbAction>

}