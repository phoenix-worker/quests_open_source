@file:Suppress("unused")

package apps.cradle.quests.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.cradle.quests.database.entities.DbQuest

@Dao
interface QuestsDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(dbQuest: DbQuest)

    @Query("DELETE FROM quests WHERE id = :questId")
    fun deleteQuestById(questId: String): Int

    @Query("SELECT * FROM quests WHERE id = :questId")
    fun getQuest(questId: String): DbQuest?

    @Query("SELECT * FROM quests")
    fun getAll(): List<DbQuest>

    @Query("SELECT * FROM quests WHERE category_id = :categoryId")
    fun getQuestsInCategory(categoryId: String): List<DbQuest>

    @Query("UPDATE quests SET title = :title WHERE id = :questId")
    fun setQuestTitle(questId: String, title: String)

    @Query("UPDATE quests SET category_id = :categoryId WHERE id = :questId")
    fun setQuestCategory(questId: String, categoryId: String)

}