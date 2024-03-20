package apps.cradle.quests.ui.fragments.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbCategory
import apps.cradle.quests.models.QuestState
import apps.cradle.quests.models.TaskItem
import apps.cradle.quests.models.TodayEmptyElement
import apps.cradle.quests.models.toQuestsElements
import apps.cradle.quests.utils.ScheduleUtils
import apps.cradle.quests.utils.events.Event
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleViewModel : ViewModel() {

    private val _taskItems = MutableLiveData<List<TaskItem>>()
    val taskItems: LiveData<List<TaskItem>> = _taskItems
    private var shouldSendScrollEvent = false

    fun updateSchedule() {
        viewModelScope.launch(Dispatchers.IO) {
            val prevTaskItems = _taskItems.value ?: listOf()
            val newTaskItems = ScheduleUtils.getTaskItemsForMainScreen()
            _taskItems.postValue(newTaskItems)
            checkShouldSendScrollEvent(prevTaskItems, newTaskItems)
            updateHasFinishedQuests()
        }
    }

    private val _scrollToBottomEvent = MutableLiveData<Event<Boolean>>()
    val scrollToBottomEvent = _scrollToBottomEvent.toLiveData()

    fun sendScrollToBottomEvent() {
        _scrollToBottomEvent.value = Event(false)
    }

    fun sendScrollToBottomEventIfRequired() {
        if (shouldSendScrollEvent) {
            shouldSendScrollEvent = false
            _scrollToBottomEvent.postValue(Event(true))
        }
    }

    private fun checkShouldSendScrollEvent(prevList: List<TaskItem>, currentList: List<TaskItem>) {
        val todayEmptyWasNotInPrevious = prevList.firstOrNull { it is TodayEmptyElement } == null
        val todayEmpty = currentList.firstOrNull { it is TodayEmptyElement }
        val todayEmptyIsInCurrent = todayEmpty != null
        shouldSendScrollEvent = todayEmptyWasNotInPrevious && todayEmptyIsInCurrent
    }

    enum class MenuItemEvent { QUICK_TASK, QUICK_NOTE, DATABASE }

    private val _menuItemEvent = MutableLiveData<Event<MenuItemEvent>>()
    val menuItemEvent = _menuItemEvent.toLiveData()

    fun sendMenuItemEvent(menuItem: MenuItemEvent) {
        _menuItemEvent.value = Event(menuItem)
    }

    private val _hasFinishedQuests = MutableLiveData<Boolean>()
    val hasFinishedQuests = _hasFinishedQuests.toLiveData()

    fun updateHasFinishedQuests() {
        viewModelScope.launch(Dispatchers.IO) {
            val quests = App.db.questsDao().getAll().toQuestsElements()
            _hasFinishedQuests.postValue(
                quests.any {
                    it.state == QuestState.FINISHED && it.categoryId != DbCategory.ARCHIVE_CATEGORY
                }
            )
        }
    }
}