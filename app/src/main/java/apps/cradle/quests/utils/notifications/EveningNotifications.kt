package apps.cradle.quests.utils.notifications

import android.app.PendingIntent
import android.content.Intent
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.receivers.NotificationsActionsReceiver
import apps.cradle.quests.utils.TasksUtils

fun showEveningNotification() {
    val context = App.instance
    val todayTasks = TasksUtils.getTodayTasks()
    if (todayTasks.isNotEmpty()) {
        val message = getEveningNotificationMessage(todayTasks)
        val notificationId = EVENING_NOTIFICATION_ID
        val notification = getBigTextNotification(
            context = context,
            title = context.getString(R.string.eveningNotificationTitle),
            message = message,
            actionTitle = context.getString(R.string.eveningNotificationAction),
            actionPendingIntent = getPendingIntentForAction(),
            contentPendingIntent = getPendingIntentForMainActivity(),
            timeOut = getDailyNotificationTimeout()
        )
        showNotificationIfHavePermission(context, notificationId, notification)
    }
}

private fun getPendingIntentForAction(): PendingIntent {
    val intent = Intent(App.instance, NotificationsActionsReceiver::class.java)
    intent.putExtra(NotificationsActionsReceiver.EXTRA_NOTIFICATION_ID, EVENING_NOTIFICATION_ID)
    intent.putExtra(
        NotificationsActionsReceiver.EXTRA_ACTION,
        NotificationsActionsReceiver.ACTION_MOVE_TODAY_TASKS_TO_TOMORROW
    )
    val flags = PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(App.instance, 143, intent, flags)
}

private fun getEveningNotificationMessage(tasks: List<DbTask>): String {
    val context = App.instance
    val count = tasks.size
    val builder = StringBuilder(
        context.resources.getQuantityString(
            R.plurals.eveningNotificationMessage,
            count,
            count.toString()
        )
    )
    for (task in tasks) {
        builder.append('\n')
        builder.append("- ")
        builder.append(task.title)
    }
    return builder.toString()
}