package apps.cradle.quests.utils

import apps.cradle.quests.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

object NotesUtils {

    fun changeNoteQuest(noteId: String, newQuestId: String) = runBlocking(Dispatchers.IO) {
        App.db.notesDao().changeNoteQuest(noteId, newQuestId)
    }

    fun deleteNote(noteId: String) = runBlocking(Dispatchers.IO) {
        App.db.notesDao().deleteNoteById(noteId)
    }
}