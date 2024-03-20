package apps.cradle.quests.ui.fragments.tasks.task

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.toActions
import apps.cradle.quests.ui.fragments.tasks.TaskWithActionsViewModel
import apps.cradle.quests.utils.cancelScheduledAlarmsForTask
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TaskViewModel : TaskWithActionsViewModel() {

    private val _task = MutableLiveData<DbTask>()
    val task = _task.toLiveData()
    private val _quest = MutableLiveData<DbQuest>()
    val quest = _quest.toLiveData()

    fun initialize(taskId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.tasksDao().getTask(taskId.orEmpty())?.let { task ->
                _task.postValue(task)
                _date.postValue(task.date)
                _time.postValue(task.time)
                _deadline.postValue(task.deadline)
                _quest.postValue(App.db.questsDao().getQuest(task.questId))
                updateActions(taskId.orEmpty())
            }
        }
    }

    fun renameTask(title: String) = runBlocking(Dispatchers.IO) {
        task.value?.let {
            App.db.tasksDao().setTaskTitle(taskId = it.id, title = title)
        }
    }

    private val _date = MutableLiveData<Long>()
    val date = _date.toLiveData()

    fun changeDate(newDate: Long) {
        _date.value = newDate
    }

    fun updateTaskDate() = runBlocking(Dispatchers.IO) {
        task.value?.let {
            cancelScheduledAlarmsForTask(it)
            App.db.tasksDao().setTaskDate(taskId = it.id, date = _date.value ?: it.date)
        }
    }

    private val _time = MutableLiveData<Long>()
    val time = _time.toLiveData()

    fun changeTime(newTime: Long) {
        _time.value = newTime
    }

    fun updateTaskTime() = runBlocking(Dispatchers.IO) {
        task.value?.let {
            cancelScheduledAlarmsForTask(it)
            App.db.tasksDao().setTaskTime(taskId = it.id, time = _time.value ?: it.time)
        }
    }

    private val _deadline = MutableLiveData<Long>()
    val deadline = _deadline.toLiveData()

    fun changeDeadline(newDeadline: Long) {
        _deadline.value = newDeadline
    }

    fun updateTaskDeadline() = runBlocking(Dispatchers.IO) {
        task.value?.let {
            App.db.tasksDao().setTaskDeadline(
                taskId = it.id,
                deadline = _deadline.value ?: throw IllegalArgumentException()
            )
        }
    }

    fun updateTaskReminder(newReminder: Long) = runBlocking(Dispatchers.IO) {
        task.value?.let {
            cancelScheduledAlarmsForTask(it)
            App.db.tasksDao().setTaskReminder(
                taskId = it.id,
                reminder = newReminder
            )
        }
    }

    private fun updateActions(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _actions.postValue(
                App.db.actionsDao().getAllTaskActions(taskId).toActions()
                    .sortedBy { it.state }
            )
        }
    }

    private val _exitEvent = MutableLiveData<EmptyEvent>()
    val exitEvent = _exitEvent.toLiveData()

    fun sendExitEvent() {
        _exitEvent.postValue(EmptyEvent())
    }

}