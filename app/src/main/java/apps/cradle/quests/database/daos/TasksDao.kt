package apps.cradle.quests.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.cradle.quests.database.entities.DbTask

@Dao
interface TasksDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(dbTask: DbTask): Long

    @Query("DELETE FROM tasks WHERE id = :taskId")
    fun deleteTaskById(taskId: String): Int

    @Query("SELECT * FROM tasks WHERE state != ${DbTask.STATE_FINISHED}")
    fun getAllActiveTasks(): List<DbTask>

    @Query("SELECT * FROM tasks WHERE quest_id = :questId")
    fun getAllQuestTasks(questId: String): List<DbTask>

    @Query("SELECT * FROM tasks WHERE quest_id = :questId AND state = ${DbTask.STATE_ACTIVE}")
    fun getQuestActiveTasks(questId: String): List<DbTask>

    @Query("UPDATE tasks SET state = ${DbTask.STATE_FINISHED} WHERE id = :taskId")
    fun finishTask(taskId: String)

    @Query("UPDATE tasks SET state = ${DbTask.STATE_ACTIVE} WHERE id = :taskId")
    fun resumeTask(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTask(taskId: String): DbTask?

    @Query("UPDATE tasks SET date = :date WHERE id = :taskId")
    fun updateTaskDate(taskId: String, date: Long)

    @Query("DELETE FROM tasks WHERE quest_id = :questId")
    fun deleteAllQuestTasks(questId: String)

    @Query("UPDATE tasks SET title = :title WHERE id = :taskId")
    fun setTaskTitle(taskId: String, title: String)

    @Query("UPDATE tasks SET date = :date WHERE id = :taskId")
    fun setTaskDate(taskId: String, date: Long)

    @Query("UPDATE tasks SET time = :time WHERE id = :taskId")
    fun setTaskTime(taskId: String, time: Long)

    @Query("UPDATE tasks SET deadline = :deadline WHERE id = :taskId")
    fun setTaskDeadline(taskId: String, deadline: Long)

    @Query("UPDATE tasks SET reminder = :reminder WHERE id = :taskId")
    fun setTaskReminder(taskId: String, reminder: Long)

    @Query("SELECT * FROM tasks WHERE state = ${DbTask.STATE_ACTIVE} AND date >= :dateStart AND date < :dateEnd")
    fun getActiveTasksForAPeriod(dateStart: Long, dateEnd: Long): List<DbTask>

    @Query("SELECT * FROM tasks WHERE state = ${DbTask.STATE_ACTIVE} AND time <> ${DbTask.NO_TIME} AND (date + time) > :timePoint AND reminder <> ${DbTask.REMINDER_DEFAULT}")
    fun getActiveTasksWithRemindersAfterTimePoint(timePoint: Long): List<DbTask>

    @Query("SELECT * FROM tasks WHERE state = ${DbTask.STATE_ACTIVE} AND time <> ${DbTask.NO_TIME} AND (date + time) > :timePoint")
    fun getActiveTasksWithTimeAfterTimePoint(timePoint: Long): List<DbTask>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :clause || '%' COLLATE NOCASE")
    fun searchTasks(clause: String): List<DbTask>

    @Query("UPDATE tasks SET quest_id = :newQuestId WHERE id = :taskId")
    fun changeTaskQuest(taskId: String, newQuestId: String)

}