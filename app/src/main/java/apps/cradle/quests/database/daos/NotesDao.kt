package apps.cradle.quests.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.cradle.quests.database.entities.DbNote

@Dao
interface NotesDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(dbNote: DbNote)

    @Query("DELETE FROM notes WHERE id = :noteId")
    fun deleteNoteById(noteId: String)

    @Query("DELETE FROM notes WHERE quest_id = :questId")
    fun deleteAllQuestNotes(questId: String)

    @Query("SELECT * FROM notes WHERE quest_id = :questId")
    fun getQuestNotes(questId: String): List<DbNote>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNote(noteId: String): DbNote?

    @Query("UPDATE notes SET title = :title, content = :content WHERE id = :noteId")
    fun updateNote(noteId: String, title: String, content: String)

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :clause || '%' COLLATE NOCASE OR content LIKE '%' || :clause || '%' COLLATE NOCASE")
    fun searchNotes(clause: String): List<DbNote>

    @Query("UPDATE notes SET quest_id = :newQuestId WHERE id = :noteId")
    fun changeNoteQuest(noteId: String, newQuestId: String)

}