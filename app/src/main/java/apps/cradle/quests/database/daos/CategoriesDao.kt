package apps.cradle.quests.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import apps.cradle.quests.database.entities.DbCategory

@Dao
interface CategoriesDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insert(dbCategory: DbCategory)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    fun deleteCategoryById(categoryId: String): Int

    @Query("SELECT * FROM categories")
    fun getAll(): List<DbCategory>

    @Query("UPDATE categories SET title = :categoryTitle WHERE id = :categoryId")
    fun setCategoryTitle(categoryId: String, categoryTitle: String)

}