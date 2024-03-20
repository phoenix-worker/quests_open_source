package apps.cradle.quests.ui.fragments.notes.editNote

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.database.entities.DbNote
import apps.cradle.quests.utils.events.EmptyEvent
import apps.cradle.quests.utils.toLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditNoteViewModel : ViewModel() {

    private val _exitEvent = MutableLiveData<EmptyEvent>()
    val exitEvent = _exitEvent.toLiveData()

    private val _note = MutableLiveData<DbNote>()
    val note = _note.toLiveData()

    fun initialize(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _note.postValue(App.db.notesDao().getNote(noteId))
        }
    }

    fun updateNoteAndExit(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _note.value?.id?.let { noteId ->
                App.db.notesDao().updateNote(
                    noteId = noteId,
                    title = title,
                    content = content
                )
                _exitEvent.postValue(EmptyEvent())
            }
        }
    }

}