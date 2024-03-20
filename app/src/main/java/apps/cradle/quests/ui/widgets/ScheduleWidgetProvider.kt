package apps.cradle.quests.ui.widgets

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.getActivity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.ui.activities.MainActivity
import apps.cradle.quests.ui.adapters.TaskItemsAdapter.ScheduleSummaryVH.Companion.getFutureTasksText
import apps.cradle.quests.ui.adapters.TaskItemsAdapter.ScheduleSummaryVH.Companion.getStaleTasksText
import apps.cradle.quests.ui.adapters.TaskItemsAdapter.ScheduleSummaryVH.Companion.getTodayTasksText
import apps.cradle.quests.utils.resetTimeInMillis

class ScheduleWidgetProvider : AppWidgetProvider() {

    private lateinit var remoteViews: RemoteViews
    private val image = R.id.image
    private val counter = R.id.counter
    private val today = R.id.today
    private val stale = R.id.stale
    private val future = R.id.future

    override fun onReceive(context: Context?, intent: Intent?) {
        App.log("ScheduleWidgetProvider received intent: ${intent?.action}")
        if (intent?.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val ids =
                AppWidgetManager.getInstance(App.instance.applicationContext).getAppWidgetIds(
                    ComponentName(
                        App.instance.applicationContext,
                        this::class.java
                    )
                )
            context?.run {
                onUpdate(this, AppWidgetManager.getInstance(this), ids)
            }
        } else super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        appWidgetIds?.forEach {
            val intent = Intent(context, MainActivity::class.java)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) FLAG_MUTABLE else 0
            val pendingIntent = getActivity(context, 0, intent, flags)
            setDataToView(context, pendingIntent)
            appWidgetManager?.updateAppWidget(it, remoteViews)
        }
    }

    private fun setDataToView(
        context: Context,
        pending: PendingIntent
    ) {
        remoteViews = RemoteViews(context.packageName, R.layout.widget_schedule)
        remoteViews.setOnClickPendingIntent(android.R.id.background, pending)
        val todayMs = resetTimeInMillis(System.currentTimeMillis())
        val tasks = runBlocking(Dispatchers.IO) { App.db.tasksDao().getAllActiveTasks() }
        val todayTasksCount = tasks.count { resetTimeInMillis(it.date) == todayMs }
        val staleTasksCount = tasks.count { resetTimeInMillis(it.date) < todayMs }
        val futureTasksCount = tasks.count { resetTimeInMillis(it.date) > todayMs }
        setupTodayInfo(todayTasksCount, context)
        when {
            staleTasksCount > 0 -> {
                remoteViews.setTextViewText(stale, getStaleTasksText(context, staleTasksCount))
                remoteViews.setViewVisibility(stale, View.VISIBLE)
            }

            else -> {
                remoteViews.setViewVisibility(stale, View.GONE)
            }
        }
        remoteViews.setTextViewText(future, getFutureTasksText(context, futureTasksCount))
        setupCounterBackground(tasks, staleTasksCount)
    }

    private fun setupCounterBackground(tasks: List<DbTask>, staleTasksCount: Int) {
        val resId = when {
            staleTasksCount > 0 -> R.drawable.background_circle_error
            else -> {
                val nowMs = System.currentTimeMillis()
                val todayTasksWithTime = tasks
                    .filter { resetTimeInMillis(it.date) == resetTimeInMillis(nowMs) }
                    .filter { it.time != DbTask.NO_TIME }
                if (todayTasksWithTime.any { it.date + it.time < nowMs }) R.drawable.background_circle_accent
                else R.drawable.background_circle_green
            }
        }
        remoteViews.setInt(counter, "setBackgroundResource", resId)
    }

    private fun setupTodayInfo(todayTasksCount: Int, context: Context) {
        when {
            todayTasksCount > 0 -> {
                remoteViews.setViewVisibility(image, View.GONE)
                remoteViews.setViewVisibility(counter, View.VISIBLE)
                val counterText = if (todayTasksCount > 9) "9+" else "$todayTasksCount"
                remoteViews.setTextViewText(counter, counterText)
                val todayTasksText = when {
                    todayTasksCount > 9 -> {
                        context.getString(R.string.todayTasksOverflowScheduleWidget)
                    }

                    else -> getTodayTasksText(
                        context,
                        todayTasksCount,
                        R.plurals.todayTasksCountScheduleWidget
                    )
                }
                remoteViews.setTextViewText(today, todayTasksText)
            }

            else -> {
                remoteViews.setViewVisibility(counter, View.GONE)
                remoteViews.setViewVisibility(image, View.VISIBLE)
                remoteViews.setTextViewText(today, context.getString(R.string.noTodayTasks))
            }
        }
    }

}