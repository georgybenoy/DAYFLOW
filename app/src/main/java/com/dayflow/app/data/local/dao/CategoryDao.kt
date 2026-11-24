package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dayflow.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    fun getCategoryById(id: String): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}
