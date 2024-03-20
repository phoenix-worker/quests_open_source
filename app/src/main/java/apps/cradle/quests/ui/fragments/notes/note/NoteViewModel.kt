package apps.cradle.quests.ui.fragments.notes.note

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apps.cradle.quests.App
import apps.cradle.quests.models.NoteElement
import apps.cradle.quests.models.toNoteElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class NoteViewModel : ViewModel() {

    private val _noteFlow = MutableSharedFlow<NoteElement>(replay = 1)
    val noteFlow: SharedFlow<NoteElement> = _noteFlow

    fun loadNote(noteId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            App.db.notesDao().getNote(noteId)?.let {
                _noteFlow.emit(it.toNoteElement())
            }
        }
    }
}