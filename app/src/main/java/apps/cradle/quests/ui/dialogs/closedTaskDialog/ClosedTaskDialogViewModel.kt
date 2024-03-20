package apps.cradle.quests.ui.dialogs.closedTaskDialog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.models.TaskElement
import apps.cradle.quests.utils.ScheduleUtils
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClosedTaskDialogViewModel : ViewModel() {

    private val _task = MutableLiveData<TaskElement?>()
    val task = _task.toLiveData()

    fun loadTask(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = App.db.tasksDao().getTask(taskId)?.let { dbTask ->
                ScheduleUtils.createTaskElement(
                    dbTask,
                    ScheduleUtils.getQuestsMap(),
                    ScheduleUtils.getTasksActionsMap()
                )
            }
            _task.postValue(task)
            loadActions(taskId)
        }
    }

    private val _actions = MutableLiveData<List<String>>()
    val actions = _actions.toLiveData()

    private fun loadActions(taskId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val actions = App.db.actionsDao().getAllTaskActions(taskId).map { it.title }
            _actions.postValue(actions)
        }
    }
}