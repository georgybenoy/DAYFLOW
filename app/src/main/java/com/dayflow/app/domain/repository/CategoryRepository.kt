package com.dayflow.app.domain.repository

import com.dayflow.app.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(id: String): Flow<Category?>
    suspend fun saveCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}
