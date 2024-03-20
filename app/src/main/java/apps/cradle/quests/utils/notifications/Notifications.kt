package apps.cradle.quests.utils.notifications

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import apps.cradle.quests.App
import apps.cradle.quests.BuildConfig
import apps.cradle.quests.R
import apps.cradle.quests.ui.activities.MainActivity
import apps.cradle.quests.utils.UniqueIdGenerator
import apps.cradle.quests.utils.getTomorrow
import java.util.Calendar
import java.util.Locale

const val MORNING_NOTIFICATION_ID = 8501
const val EVENING_NOTIFICATION_ID = 8502

const val MORNING_ALARM_REQUEST_CODE = 7501
const val EVENING_ALARM_REQUEST_CODE = 7502
const val MIDNIGHT_ALARM_REQUEST_CODE = 7503

fun getBigTextNotification(
    context: Context,
    title: String,
    message: String,
    actionTitle: String? = null,
    actionPendingIntent: PendingIntent? = null,
    contentPendingIntent: PendingIntent,
    timeOut: Long? = null
): Notification {
    return NotificationCompat.Builder(context, MainActivity.NOTIFICATIONS_CHANNEL_ID_DAILY).apply {
        setSmallIcon(R.drawable.ic_notification)
        setContentTitle(title)
        setContentText(message)
        setAutoCancel(true)
        setShowWhen(false)
        setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(message))
        actionTitle?.let { title ->
            val builder = NotificationCompat.Action.Builder(0, title, actionPendingIntent)
            addAction(builder.build())
        }
        setContentIntent(contentPendingIntent)
        timeOut?.let { setTimeoutAfter(it) }
    }.build()
}

fun getReminderNotification(
    context: Context,
    title: String,
    message: String,
    contentPendingIntent: PendingIntent,
): Notification {
    return NotificationCompat.Builder(context, MainActivity.NOTIFICATIONS_CHANNEL_ID_DAILY).apply {
        setSmallIcon(R.drawable.ic_notification)
        setContentTitle(title)
        setContentText(message)
        setAutoCancel(true)
        setShowWhen(true)
        setContentIntent(contentPendingIntent)
    }.build()
}

fun getPendingIntentForMainActivity(): PendingIntent {
    val context = App.instance
    val flags = PendingIntent.FLAG_IMMUTABLE
    val intent = Intent(context, MainActivity::class.java)
    return PendingIntent.getActivity(
        context,
        UniqueIdGenerator.nextId().toInt(),
        intent,
        flags
    )
}

fun getMorningNotificationCalendar(): Calendar {
    return Calendar.getInstance(Locale.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 6)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

fun getEveningNotificationCalendar(): Calendar {
    return Calendar.getInstance(Locale.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 21)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

fun getMidnightNotificationCalendar(): Calendar {
    return Calendar.getInstance(Locale.getDefault()).apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

fun getDailyNotificationTimeout(): Long {
    val calendar = getEveningNotificationCalendar()
    val midnight = getTomorrow(calendar.time)
    return midnight.time - calendar.timeInMillis
}

fun getDebugNotification(
    context: Context,
    title: String,
    message: String
): Notification {
    return NotificationCompat.Builder(context, MainActivity.NOTIFICATIONS_CHANNEL_ID_DEBUG).apply {
        setSmallIcon(R.drawable.ic_notification)
        setContentTitle(title)
        setContentText(message)
        setShowWhen(true)
    }.build()
}

fun showDebugNotification(title: String, message: String) {
    if (BuildConfig.DEBUG) {
        val context = App.instance
        val notification = getDebugNotification(
            context = context,
            title = title,
            message = message
        )
        val notificationId = UniqueIdGenerator.nextId().toInt()
        showNotificationIfHavePermission(context, notificationId, notification)
    }
}

fun showNotificationIfHavePermission(
    context: Context,
    notificationId: Int,
    notification: Notification
) {
    val manager = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        when (checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        ) {
            PackageManager.PERMISSION_GRANTED -> manager.notify(notificationId, notification)
        }
    } else manager.notify(notificationId, notification)
}