package apps.cradle.quests.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import apps.cradle.quests.App
import apps.cradle.quests.utils.getTomorrow
import apps.cradle.quests.utils.resetTimeInMillis
import java.util.*

class NotificationsActionsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationId = intent?.extras?.getInt(EXTRA_NOTIFICATION_ID)
        notificationId?.let {
            NotificationManagerCompat.from(context ?: App.instance).cancel(it)
        }
        when (intent?.extras?.getString(EXTRA_ACTION)) {
            ACTION_MOVE_TODAY_TASKS_TO_TOMORROW -> moveTodayTasksToTomorrow()
        }
    }

    private fun moveTodayTasksToTomorrow() {
        CoroutineScope(Dispatchers.IO).launch {
            val currentDate = Date()
            val today = resetTimeInMillis(currentDate.time)
            val tomorrow = getTomorrow(currentDate).time
            val todayTasks = App.db.tasksDao().getActiveTasksForAPeriod(today, tomorrow)
            for (task in todayTasks) App.db.tasksDao().updateTaskDate(task.id, tomorrow)
        }
    }

    companion object {

        const val EXTRA_ACTION = "extra_action"
        const val ACTION_MOVE_TODAY_TASKS_TO_TOMORROW = "action_move_today_tasks_to_tomorrow"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    }

}