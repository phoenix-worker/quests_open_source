package apps.cradle.quests.ui.fragments.tasks

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbAction.Companion.STATE_ACTIVE
import apps.cradle.quests.database.entities.DbAction.Companion.STATE_FINISHED
import apps.cradle.quests.models.Action
import apps.cradle.quests.utils.EMPTY
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.UUID

abstract class TaskWithActionsViewModel : ViewModel() {

    protected val _actions = MutableLiveData<List<Action>>(listOf())
    val actions = _actions.toLiveData()

    fun addActions(input: String) {
        _actions.postValue(
            (_actions.value ?: listOf())
                .toMutableList()
                .apply { addAll(getNewActionsFromInputString(input)) }
                .sortedBy { it.state }
        )
    }

    fun deleteAction(actionId: String) {
        _actions.postValue(
            _actions.value
                ?.filter { it.id != actionId }
                ?.sortedBy { it.state }
        )
    }

    fun toggleAction(action: Action) {
        val state = if (action.state == STATE_ACTIVE) STATE_FINISHED else STATE_ACTIVE
        _actions.postValue(
            _actions.value?.map {
                Action(
                    id = it.id,
                    taskId = it.taskId,
                    title = it.title,
                    state = if (it.id == action.id) state else it.state,
                )
            }?.sortedBy { it.state }
        )
    }

    fun updateTaskActions(taskId: String) = runBlocking(Dispatchers.IO) {
        App.db.actionsDao().deleteAllTaskActions(taskId)
        TasksUtils.createTaskActions(_actions.value ?: listOf(), taskId)
    }

    private fun getNewActionsFromInputString(input: String): List<Action> {
        return if (input.startsWith("Список:", true)) {
            input
                .split(':')
                .last()
                .split(',')
                .filter { it.isNotBlank() }
                .map {
                    Action(
                        id = UUID.randomUUID().toString(),
                        taskId = EMPTY,
                        title = it.trim(),
                        state = STATE_ACTIVE
                    )
                }
        } else {
            listOf(
                Action(
                    id = UUID.randomUUID().toString(),
                    taskId = EMPTY,
                    title = input,
                    state = STATE_ACTIVE
                )
            )
        }
    }
}