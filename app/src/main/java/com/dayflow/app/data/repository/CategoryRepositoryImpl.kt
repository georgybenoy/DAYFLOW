package com.dayflow.app.data.repository

import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.data.local.dao.CategoryDao
import com.dayflow.app.data.mapper.toDomain
import com.dayflow.app.data.mapper.toEntity
import com.dayflow.app.domain.model.Category
import com.dayflow.app.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getCategoryById(id: String): Flow<Category?> {
        return categoryDao.getCategoryById(id).map { it?.toDomain() }
    }

    override suspend fun saveCategory(category: Category) = withContext(ioDispatcher) {
        categoryDao.insertCategory(category.toEntity())
        Unit
    }

    override suspend fun deleteCategory(category: Category) = withContext(ioDispatcher) {
        categoryDao.deleteCategory(category.toEntity())
    }
}
