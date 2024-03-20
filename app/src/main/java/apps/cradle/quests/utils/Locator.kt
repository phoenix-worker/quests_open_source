package apps.cradle.quests.utils

import androidx.lifecycle.MutableLiveData
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.events.Event

object Locator {

    private val _conditionsChanged = MutableLiveData<EmptyEvent>()
    val conditionsChanged = _conditionsChanged.toLiveData()

    fun sendConditionsChangedEvent() {
        _conditionsChanged.postValue(EmptyEvent())
    }

    private val _openQuestEvent = MutableLiveData<Event<String>>()
    val openQuestEvent = _openQuestEvent.toLiveData()

    fun sendOpenQuestEvent(questId: String) {
        _openQuestEvent.postValue(Event(questId))
    }

}