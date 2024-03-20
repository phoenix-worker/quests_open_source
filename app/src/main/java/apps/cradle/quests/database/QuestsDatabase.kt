@file:Suppress("ClassName")

package apps.cradle.quests.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import apps.cradle.quests.App
import apps.cradle.quests.R
import apps.cradle.quests.database.daos.*
import apps.cradle.quests.database.entities.*

@Database(
    entities = [DbQuest::class, DbTask::class, DbCategory::class, DbAction::class, DbNote::class],
    version = 10,
    exportSchema = false
)
abstract class QuestsDatabase : RoomDatabase() {

    abstract fun questsDao(): QuestsDao

    abstract fun tasksDao(): TasksDao

    abstract fun categoriesDao(): CategoriesDao

    abstract fun actionsDao(): ActionsDao

    abstract fun notesDao(): NotesDao

    companion object {

        const val dbName = "quests_database"

        val createHeapQuestCallback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                val cursor = db.query("SELECT * FROM quests WHERE id='${DbQuest.HEAP_QUEST_ID}'")
                val title = App.instance.getString(R.string.heapQuestTitle)
                if (cursor.count == 0) {
                    db.execSQL("INSERT INTO quests (id, category_id, title) VALUES ('${DbQuest.HEAP_QUEST_ID}', '${DbCategory.WITHOUT_CATEGORY}', '$title')")
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE tasks SET quest_id = '${DbQuest.HEAP_QUEST_ID}' WHERE quest_id = 'quick_tasks'")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE from categories WHERE id='${DbCategory.WITHOUT_CATEGORY}'")
            }
        }

    }

}