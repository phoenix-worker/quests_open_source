package apps.cradle.quests.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getBroadcast
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.receivers.AlarmsReceiver
import apps.cradle.quests.utils.notifications.EVENING_ALARM_REQUEST_CODE
import apps.cradle.quests.utils.notifications.MIDNIGHT_ALARM_REQUEST_CODE
import apps.cradle.quests.utils.notifications.MORNING_ALARM_REQUEST_CODE
import apps.cradle.quests.utils.notifications.getEveningNotificationCalendar
import apps.cradle.quests.utils.notifications.getMidnightNotificationCalendar
import apps.cradle.quests.utils.notifications.getMorningNotificationCalendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.Calendar

fun scheduleAlarms() {
    scheduleMorningAlarm()
    scheduleEveningAlarm()
    scheduleTasksReminders()
    scheduleTasksAlarms()
    scheduleMidnightAlarm()
}

private const val pendingIntentFlags = FLAG_MUTABLE or FLAG_UPDATE_CURRENT

private fun scheduleEveningAlarm() {
    val intent = Intent(App.instance, AlarmsReceiver::class.java)
    intent.putExtra(AlarmsReceiver.EXTRA_ALARM_TYPE, AlarmsReceiver.ALARM_TYPE_EVENING)
    val requestCode = EVENING_ALARM_REQUEST_CODE
    val pendingIntent = getBroadcast(App.instance, requestCode, intent, pendingIntentFlags)
    val calendar = getEveningNotificationCalendar()
    if (System.currentTimeMillis() >= calendar.timeInMillis) calendar.add(Calendar.DAY_OF_MONTH, 1)
    scheduleExactAlarm(calendar.timeInMillis, pendingIntent)
}

private fun scheduleMorningAlarm() {
    val intent = Intent(App.instance, AlarmsReceiver::class.java)
    intent.putExtra(AlarmsReceiver.EXTRA_ALARM_TYPE, AlarmsReceiver.ALARM_TYPE_MORNING)
    val requestCode = MORNING_ALARM_REQUEST_CODE
    val pendingIntent = getBroadcast(App.instance, requestCode, intent, pendingIntentFlags)
    val calendar = getMorningNotificationCalendar()
    if (System.currentTimeMillis() >= calendar.timeInMillis) calendar.add(Calendar.DAY_OF_MONTH, 1)
    scheduleExactAlarm(calendar.timeInMillis, pendingIntent)
}

private fun scheduleTasksReminders() = runBlocking(Dispatchers.IO) {
    val tasks =
        App.db.tasksDao().getActiveTasksWithRemindersAfterTimePoint(System.currentTimeMillis())
    for (task in tasks) {
        val reminderTime = task.date + task.time - task.reminder
        val currentTime = System.currentTimeMillis()
        if (reminderTime > currentTime) scheduleExactAlarm(
            reminderTime,
            getPendingIntentForTaskReminder(task)
        )
    }
}

private fun scheduleTasksAlarms() = runBlocking(Dispatchers.IO) {
    val tasks = App.db.tasksDao().getActiveTasksWithTimeAfterTimePoint(System.currentTimeMillis())
    for (task in tasks) {
        val reminderTime = task.date + task.time
        val currentTime = System.currentTimeMillis()
        if (reminderTime > currentTime) scheduleExactAlarm(
            reminderTime,
            getPendingIntentForTaskAlarm(task)
        )
    }
}

private fun scheduleMidnightAlarm() {
    val intent = Intent(App.instance, AlarmsReceiver::class.java)
    intent.putExtra(AlarmsReceiver.EXTRA_ALARM_TYPE, AlarmsReceiver.ALARM_TYPE_MIDNIGHT)
    val requestCode = MIDNIGHT_ALARM_REQUEST_CODE
    val pendingIntent = getBroadcast(App.instance, requestCode, intent, pendingIntentFlags)
    val calendar = getMidnightNotificationCalendar()
    scheduleExactAlarm(calendar.timeInMillis, pendingIntent)
}

private fun scheduleExactAlarm(time: Long, pendingIntent: PendingIntent) {
    val alarmManager = App.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    if (VERSION.SDK_INT >= VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) return
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
}

private fun getPendingIntentForTaskReminder(task: DbTask): PendingIntent {
    val intent = Intent(App.instance, AlarmsReceiver::class.java)
    intent.putExtra(AlarmsReceiver.EXTRA_ALARM_TYPE, AlarmsReceiver.ALARM_TYPE_TASK_REMINDER)
    intent.putExtra(AlarmsReceiver.EXTRA_TASK_ID, task.id)
    val requestCode = task.id.hashCode() + (task.time + task.reminder).hashCode()
    return getBroadcast(App.instance, requestCode, intent, pendingIntentFlags)
}

private fun getPendingIntentForTaskAlarm(task: DbTask): PendingIntent {
    val intent = Intent(App.instance, AlarmsReceiver::class.java)
    intent.putExtra(AlarmsReceiver.EXTRA_ALARM_TYPE, AlarmsReceiver.ALARM_TYPE_TASK_EVENT)
    intent.putExtra(AlarmsReceiver.EXTRA_TASK_ID, task.id)
    val requestCode = task.id.hashCode()
    return getBroadcast(App.instance, requestCode, intent, pendingIntentFlags)
}

fun cancelScheduledAlarmsForTask(task: DbTask) {
    val alarmManager = App.instance.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(getPendingIntentForTaskReminder(task))
    alarmManager.cancel(getPendingIntentForTaskAlarm(task))
}