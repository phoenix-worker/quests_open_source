package apps.cradle.quests.utils

import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbAction
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.Action
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Date

object TasksUtils {

    fun createNewTask(newTask: DbTask) {
        App.db.tasksDao().insert(newTask)
        Locator.sendConditionsChangedEvent()
    }

    fun deleteTaskAndAllItsData(taskId: String) = runBlocking(Dispatchers.IO) {
        App.db.tasksDao().getTask(taskId)?.let { cancelScheduledAlarmsForTask(it) }
        App.db.tasksDao().deleteTaskById(taskId)
        App.db.actionsDao().deleteAllTaskActions(taskId)
        Locator.sendConditionsChangedEvent()
    }

    fun finishTask(taskId: String) = runBlocking(Dispatchers.IO) {
        App.db.tasksDao().getTask(taskId)?.let { cancelScheduledAlarmsForTask(it) }
        App.db.tasksDao().finishTask(taskId)
        Locator.sendConditionsChangedEvent()
    }

    fun resumeTask(taskId: String) = runBlocking(Dispatchers.IO) {
        App.db.tasksDao().resumeTask(taskId)
        Locator.sendConditionsChangedEvent()
    }

    fun getTodayTasks(): List<DbTask> = runBlocking(Dispatchers.IO) {
        val today = resetTimeInMillis(Date().time)
        val tomorrow = getTomorrow(Date()).time
        App.db.tasksDao().getActiveTasksForAPeriod(today, tomorrow)
    }

    fun changeTaskQuest(taskId: String, newQuestId: String) = runBlocking(Dispatchers.IO) {
        App.db.tasksDao().changeTaskQuest(taskId, newQuestId)
        Locator.sendConditionsChangedEvent()
    }

    fun createTaskActions(actions: List<Action>, taskId: String) {
        actions.forEach { action ->
            App.db.actionsDao().insert(
                DbAction(
                    id = action.id,
                    taskId = taskId,
                    title = action.title,
                    state = action.state
                )
            )
        }
    }
}