package apps.cradle.quests.utils.notifications

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask

fun showReminderNotification(taskId: String) {
    val context = App.instance
    getTask(taskId)?.let { task ->
        val notificationId = task.id.hashCode()
        val title = context.getString(
            R.string.reminderNotificationTitleBeginning,
            when (task.reminder) {
                DbTask.REMINDER_5_MIN -> context.getString(R.string.reminder5Min)
                DbTask.REMINDER_10_MIN -> context.getString(R.string.reminder10Min)
                DbTask.REMINDER_15_MIN -> context.getString(R.string.reminder15Min)
                DbTask.REMINDER_30_MIN -> context.getString(R.string.reminder30Min)
                DbTask.REMINDER_1_HOUR -> context.getString(R.string.reminder1Hour)
                DbTask.REMINDER_2_HOURS -> context.getString(R.string.reminder2Hours)
                else -> throw IllegalAccessError("Wrong reminder value.")
            }
        )
        val notification = getReminderNotification(
            context = context,
            title = title,
            message = task.title,
            contentPendingIntent = getPendingIntentForMainActivity()
        )
        showNotificationIfHavePermission(context, notificationId, notification)
    }
}

fun showTaskEventNotification(taskId: String) {
    val context = App.instance
    getTask(taskId)?.let { task ->
        val notificationId = task.id.hashCode()
        val title = context.getString(R.string.taskEventNotificationTitle)
        val notification = getReminderNotification(
            context = context,
            title = title,
            message = task.title,
            contentPendingIntent = getPendingIntentForMainActivity()
        )
        showNotificationIfHavePermission(context, notificationId, notification)
    }
}

private fun getTask(taskId: String): DbTask? = runBlocking(Dispatchers.IO) {
    App.db.tasksDao().getTask(taskId)
}