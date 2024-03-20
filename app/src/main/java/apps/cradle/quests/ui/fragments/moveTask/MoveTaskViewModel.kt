package apps.cradle.quests.ui.fragments.moveTask

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.utils.cancelScheduledAlarmsForTask
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.resetTimeInMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MoveTaskViewModel : ViewModel() {

    lateinit var task: DbTask

    fun initializeTask(taskId: String) = runBlocking(Dispatchers.IO) {
        task = App.db.tasksDao().getTask(taskId) ?: throw IllegalArgumentException("No task found.")
    }

    private val _taskMovedEvent = MutableLiveData<EmptyEvent>()
    val taskMovedEvent: LiveData<EmptyEvent> = _taskMovedEvent

    private fun sendTaskMovedEvent() {
        _taskMovedEvent.postValue(EmptyEvent())
    }

    fun moveTaskToToday() {
        moveTaskToDate(resetTimeInMillis(Date().time))
    }

    fun moveTaskToTomorrow() {
        moveTaskToDate(getTomorrow())
    }

    fun moveTaskToDayAfterTomorrow() {
        moveTaskToDate(getDayAfterTomorrow())
    }

    fun moveTaskToNextWeek() {
        moveTaskToDate(getNextMonday())
    }

    fun moveTaskToDate(date: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelScheduledAlarmsForTask(task)
            App.db.tasksDao().updateTaskDate(task.id, date)
            sendTaskMovedEvent()
        }
    }


    companion object {

        private const val WEEK_LENGTH_MS = 1000L * 60L * 60L * 24L * 7L

        fun getToday(): Long {
            return resetTimeInMillis(Date().time)
        }

        fun getTomorrow(): Long {
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.timeInMillis = resetTimeInMillis(Date().time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            return calendar.timeInMillis
        }

        fun getDayAfterTomorrow(): Long {
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.timeInMillis = resetTimeInMillis(Date().time)
            calendar.add(Calendar.DAY_OF_MONTH, 2)
            return calendar.timeInMillis
        }

        fun getWeekLater(): Long {
            return getToday() + WEEK_LENGTH_MS
        }

        fun getTwoWeeksLater(): Long {
            return getToday() + WEEK_LENGTH_MS * 2
        }

        fun getFourWeeksLater(): Long {
            return getToday() + WEEK_LENGTH_MS * 4
        }

        fun getNextMonday(): Long {
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.timeInMillis = resetTimeInMillis(Date().time)
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
            calendar.set(Calendar.DAY_OF_WEEK, 2)
            return calendar.timeInMillis
        }

        fun getNextMonth(): Long {
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.timeInMillis = resetTimeInMillis(Date().time)
            calendar.add(Calendar.MONTH, 1)
            return calendar.timeInMillis
        }

        fun getNextYear(): Long {
            val calendar = Calendar.getInstance(Locale.getDefault())
            calendar.timeInMillis = resetTimeInMillis(Date().time)
            calendar.add(Calendar.YEAR, 1)
            return calendar.timeInMillis
        }

    }

}