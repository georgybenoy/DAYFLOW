package com.dayflow.app.data.local.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.dayflow.app.data.local.DayflowDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DayflowDatabase::class.java
    )

    @Test
    fun schemaV1_existsAndValidates() {
        val schemaFile = File("schemas/com.dayflow.app.data.local.DayflowDatabase/1.json")
        assertTrue("Room schema file for version 1 should exist", schemaFile.exists())
        val content = schemaFile.readText()
        assertTrue(content.contains("\"version\": 1"))
        assertTrue(content.contains("\"tableName\": \"categories\""))
        assertTrue(content.contains("\"tableName\": \"reminders\""))
        assertTrue(content.contains("\"tableName\": \"reminder_occurrences\""))
        assertTrue(content.contains("\"tableName\": \"tasks\""))
        assertTrue(content.contains("\"tableName\": \"task_subtasks\""))
        assertTrue(content.contains("\"tableName\": \"habits\""))
        assertTrue(content.contains("\"tableName\": \"habit_logs\""))
        assertTrue(content.contains("\"tableName\": \"daily_plans\""))
        assertTrue(content.contains("\"tableName\": \"daily_plan_items\""))
        assertTrue(content.contains("\"tableName\": \"daily_reviews\""))
        assertTrue(content.contains("\"tableName\": \"app_settings\""))
    }

    @Test
    fun databaseV1_canBeCreatedViaHelper() {
        // Create database at version 1 using MigrationTestHelper
        val db = helper.createDatabase("test_dayflow_v1.db", 1)
        assertNotNull(db)
        db.close()
    }

    @Test
    fun migration1To2_preservesExistingData() {
        val dbName = "test_migration_1_2.db"
        val v1Db = helper.createDatabase(dbName, 1)

        // Insert pre-existing user data in version 1
        v1Db.execSQL(
            """
            INSERT INTO categories (id, name, colorHex, iconName, isDefault, createdAt)
            VALUES ('cat_1', 'Deep Work', '#A5E6D6', 'briefcase', 0, 1700000000)
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            INSERT INTO tasks (id, title, description, categoryId, priority, deadlineDate, deadlineTime, estimatedDurationMinutes, plannedDate, plannedStart, plannedEnd, status, shouldCarryForward, createdAt, completedAt, updatedAt)
            VALUES ('task_1', 'Ship Phase 12', 'Production readiness release', 'cat_1', 'HIGH', '2026-11-19', '17:00', 120, '2026-11-19', '09:00', '11:00', 'PENDING', 1, 1700000000, NULL, 1700000000)
            """.trimIndent()
        )
        // Execute migration 1 -> 2 directly on the SQLite database
        DayflowMigrations.MIGRATION_1_2.migrate(v1Db)

        // Verify pre-existing data is preserved and new column exists
        val catCursor = v1Db.query("SELECT id, name, colorHex, note FROM categories WHERE id = 'cat_1'")
        assertTrue(catCursor.moveToFirst())
        assertEquals("cat_1", catCursor.getString(0))
        assertEquals("Deep Work", catCursor.getString(1))
        assertEquals("#A5E6D6", catCursor.getString(2))
        assertTrue(catCursor.isNull(3)) // Default NULL note
        catCursor.close()

        val taskCursor = v1Db.query("SELECT id, title, priority, status FROM tasks WHERE id = 'task_1'")
        assertTrue(taskCursor.moveToFirst())
        assertEquals("task_1", taskCursor.getString(0))
        assertEquals("Ship Phase 12", taskCursor.getString(1))
        assertEquals("HIGH", taskCursor.getString(2))
        assertEquals("PENDING", taskCursor.getString(3))
        taskCursor.close()

        v1Db.close()
    }
}
