package com.dayflow.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.DayflowDatabaseCallback
import com.dayflow.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class CategoryDaoTest {

    private lateinit var database: DayflowDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .addCallback(DayflowDatabaseCallback())
            .allowMainThreadQueries()
            .build()
        categoryDao = database.categoryDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun categoryCallback_seedsSevenDefaultCategoriesIdempotently() = runBlocking {
        // Trigger db creation
        val count = categoryDao.getCategoryCount()
        assertEquals(7, count)

        val categories = categoryDao.getAllCategories().first()
        val names = categories.map { it.name }
        assertTrue(names.contains("Academics"))
        assertTrue(names.contains("Personal"))
        assertTrue(names.contains("Fitness"))
        assertTrue(names.contains("Work"))
        assertTrue(names.contains("Projects"))
        assertTrue(names.contains("Finance"))
        assertTrue(names.contains("Other"))
    }

    @Test
    fun insertAndGetCategory_worksCorrectly() = runBlocking {
        val custom = CategoryEntity(
            id = "cat_reading",
            name = "Reading",
            colorHex = "#F0E99B",
            iconName = "Book",
            isDefault = false,
            createdAt = Instant.now()
        )
        val rowId = categoryDao.insertCategory(custom)
        assertTrue(rowId > 0)

        val retrieved = categoryDao.getCategoryById("cat_reading").first()
        assertNotNull(retrieved)
        assertEquals("Reading", retrieved?.name)
    }

    @Test
    fun insertDuplicateCategoryName_isIgnoredDueToConflictStrategy() = runBlocking {
        val cat1 = CategoryEntity(
            id = "cat_test_1",
            name = "UniqueCategory",
            colorHex = "#A5E6D6",
            iconName = "Star",
            isDefault = false
        )
        val cat2 = CategoryEntity(
            id = "cat_test_2",
            name = "UniqueCategory",
            colorHex = "#F28D91",
            iconName = "Star",
            isDefault = false
        )

        categoryDao.insertCategory(cat1)
        val secondInsert = categoryDao.insertCategory(cat2)

        // OnConflictStrategy.IGNORE returns -1 for ignored inserts
        assertEquals(-1L, secondInsert)
    }

    @Test
    fun deleteCategory_removesFromDatabase() = runBlocking {
        val cat = CategoryEntity(
            id = "cat_to_delete",
            name = "Temporary",
            colorHex = "#123456",
            iconName = "Delete"
        )
        categoryDao.insertCategory(cat)
        categoryDao.deleteCategory(cat)

        val retrieved = categoryDao.getCategoryById("cat_to_delete").first()
        assertEquals(null, retrieved)
    }
}
