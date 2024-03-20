package apps.cradle.quests.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import apps.cradle.quests.App
import apps.cradle.quests.utils.notifications.showDebugNotification
import apps.cradle.quests.utils.notifications.showEveningNotification
import apps.cradle.quests.utils.notifications.showMorningNotification
import apps.cradle.quests.utils.notifications.showReminderNotification
import apps.cradle.quests.utils.notifications.showTaskEventNotification
import apps.cradle.quests.utils.updateWidgets

class AlarmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        logToFile(intent)
        intent?.let {
            val taskId = it.extras?.getString(EXTRA_TASK_ID)
            when (it.extras?.getInt(EXTRA_ALARM_TYPE)) {
                ALARM_TYPE_MORNING -> showMorningNotification()
                ALARM_TYPE_EVENING -> showEveningNotification()
                ALARM_TYPE_TASK_REMINDER -> if (taskId != null) showReminderNotification(taskId)
                ALARM_TYPE_TASK_EVENT -> {
                    if (taskId != null) showTaskEventNotification(taskId)
                    updateWidgets()
                }

                ALARM_TYPE_MIDNIGHT -> {
                    context?.sendBroadcast(Intent(ACTION_MIDNIGHT))
                    showDebugNotification("AlarmsReceiver", "Widgets update broadcast sent.")
                    updateWidgets()
                }
            }
        }
    }

    private fun logToFile(intent: Intent?) {
        val type = when (intent?.extras?.getInt(EXTRA_ALARM_TYPE)) {
            ALARM_TYPE_MORNING -> "ALARM_TYPE_MORNING"
            ALARM_TYPE_EVENING -> "ALARM_TYPE_EVENING"
            ALARM_TYPE_TASK_REMINDER -> "ALARM_TYPE_TASK_REMINDER"
            ALARM_TYPE_TASK_EVENT -> "ALARM_TYPE_TASK_EVENT"
            ALARM_TYPE_MIDNIGHT -> "ALARM_TYPE_MIDNIGHT"
            else -> "UNKNOWN"
        }
        App.fLog("AlarmsReceiver has received intent of type: $type")
    }

    companion object {

        const val EXTRA_ALARM_TYPE = "extra_alarm_type"
        const val ALARM_TYPE_MORNING = 1
        const val ALARM_TYPE_EVENING = 2
        const val ALARM_TYPE_TASK_REMINDER = 3
        const val ALARM_TYPE_TASK_EVENT = 4
        const val ALARM_TYPE_MIDNIGHT = 5

        const val EXTRA_TASK_ID = "extra_task_id"
        const val ACTION_MIDNIGHT = "apps.cradle.quests.MIDNIGHT"

    }

}