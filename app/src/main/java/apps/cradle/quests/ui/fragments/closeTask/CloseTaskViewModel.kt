package apps.cradle.quests.ui.fragments.closeTask

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import apps.cradle.quests.utils.TasksUtils
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.toLiveData

class CloseTaskViewModel : ViewModel() {

    private val _exitEvent = MutableLiveData<EmptyEvent>()
    val exitEvent = _exitEvent.toLiveData()

    fun finishTaskAndExit(taskId: String) {
        TasksUtils.finishTask(taskId)
        _exitEvent.value = EmptyEvent()
    }

}