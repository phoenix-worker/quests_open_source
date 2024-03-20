package apps.cradle.quests.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import apps.cradle.quests.App
import apps.cradle.quests.utils.notifications.showDebugNotification
import apps.cradle.quests.utils.scheduleAlarms

class AlarmsWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    override fun doWork(): Result {
        App.fLog("AlarmsWorker.doWork() is called.")
        scheduleAlarms()
        showDebugNotification("AlarmsWorker", "Alarms successfully scheduled.")
        return Result.success()
    }

}