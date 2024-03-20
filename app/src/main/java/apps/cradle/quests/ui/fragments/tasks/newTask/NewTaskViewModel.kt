package apps.cradle.quests.ui.fragments.tasks.newTask

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.database.entities.DbTask
import apps.cradle.quests.models.toActions
import apps.cradle.quests.ui.fragments.tasks.TaskWithActionsViewModel
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.events.Event
import apps.cradle.quests.utils.orThrowIAE
import apps.cradle.quests.utils.resetTimeInMillis
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.UUID

class NewTaskViewModel : TaskWithActionsViewModel() {

    private val _date = MutableLiveData(resetTimeInMillis(Date().time))
    val date: LiveData<Long> = _date

    fun changeDate(dateMillis: Long) {
        _date.value = resetTimeInMillis(dateMillis)
    }

    private val _time = MutableLiveData(DbTask.NO_TIME)
    val time = _time.toLiveData()

    fun changeTime(newTime: Long) {
        _time.value = newTime
    }

    private val _deadline = MutableLiveData(DbTask.NO_DEADLINE)
    val deadline = _deadline.toLiveData()

    fun changeDeadline(newDeadline: Long) {
        _deadline.value = newDeadline
    }

    fun createNewTask(
        questId: String,
        title: String,
        reminder: Long
    ) = runBlocking(Dispatchers.IO) {
        val newTask = constructDbTask(questId = questId, title = title, reminder = reminder)
        TasksUtils.createNewTask(newTask)
        updateTaskActions(newTask.id)
    }

    private fun constructDbTask(
        questId: String,
        title: String,
        reminder: Long
    ): DbTask {
        return DbTask(
            id = UUID.randomUUID().toString(),
            questId = questId,
            title = title,
            date = _date.value.orThrowIAE("Wrong date."),
            time = _time.value.orThrowIAE("Wrong time."),
            reminder = reminder,
            state = DbTask.STATE_ACTIVE,
            deadline = _deadline.value.orThrowIAE("Wrong deadline.")
        )
    }

    private val _taskToCopy = MutableLiveData<Event<DbTask>>()
    val taskToCopy = _taskToCopy.toLiveData()

    fun loadTaskToCopy(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.tasksDao().getTask(taskId)?.let {
                _actions.postValue(
                    App.db.actionsDao().getAllTaskActions(it.id)
                        .toActions(resetState = true, resetIds = true)
                )
                _taskToCopy.postValue(Event(it))
            }
        }
    }

    private val _questTitle = MutableLiveData<String>()
    val questTitle = _questTitle.toLiveData()

    fun loadQuestTitle(questId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (questId == DbQuest.HEAP_QUEST_ID) {
                _questTitle.postValue(App.instance.getString(R.string.heapQuestTitle))
            } else {
                App.db.questsDao().getQuest(questId)?.let { _questTitle.postValue(it.title) }
            }
        }
    }

    private val _exitEvent = MutableLiveData<EmptyEvent>()
    val exitEvent = _exitEvent.toLiveData()

    fun sendExitEvent() {
        _exitEvent.value = EmptyEvent()
    }
}