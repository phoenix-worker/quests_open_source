package apps.cradle.quests.utils.notifications

import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbAction
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.utils.ActionsUtils
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.formatTimeFromMillis

fun showMorningNotification() {
    val context = App.instance
    val todayTasks = TasksUtils.getTodayTasks()
    val actions = ActionsUtils.getAllActions()
    if (todayTasks.isNotEmpty()) {
        val message = getMorningNotificationMessage(todayTasks, actions)
        val notificationId = MORNING_NOTIFICATION_ID
        val notification = getBigTextNotification(
            context = context,
            title = context.getString(R.string.morningNotificationTitle),
            message = message,
            contentPendingIntent = getPendingIntentForMainActivity(),
            timeOut = getDailyNotificationTimeout()
        )
        showNotificationIfHavePermission(context, notificationId, notification)
    }
}

private fun getMorningNotificationMessage(tasks: List<DbTask>, actions: List<DbAction>): String {
    val context = App.instance
    val count = tasks.size
    val builder = StringBuilder(
        context.resources.getQuantityString(
            R.plurals.morningNotificationMessage,
            count,
            count.toString()
        )
    )
    for (task in tasks.sortedWith(compareBy({ it.time == DbTask.NO_TIME }, { it.time }))) {
        builder.append('\n')
        builder.append("- ")
        if (task.time != DbTask.NO_TIME) {
            builder.append(formatTimeFromMillis(task.time))
            builder.append(" ")
        }
        builder.append(task.title)
        val taskActions = actions.filter { it.taskId == task.id }
        val finishedTasksCount = taskActions.filter { it.state == DbAction.STATE_FINISHED }.size
        if (taskActions.isNotEmpty()) {
            builder.append(" ($finishedTasksCount/${taskActions.size})")
        }
    }
    return builder.toString()
}