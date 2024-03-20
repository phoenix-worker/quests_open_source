package apps.cradle.quests.ui.fragments.notes.newNote

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.entities.DbNote
import apps.cradle.quests.database.entities.DbQuest
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class NewNoteViewModel : ViewModel() {

    private val _exitEvent = MutableLiveData<EmptyEvent>()
    val exitEvent = _exitEvent.toLiveData()

    fun createNote(
        questId: String,
        noteTitle: String,
        noteContent: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.notesDao().insert(
                DbNote(
                    id = UUID.randomUUID().toString(),
                    questId = questId,
                    title = noteTitle,
                    content = noteContent,
                    created = System.currentTimeMillis()
                )
            )
            _exitEvent.postValue(EmptyEvent())
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

}