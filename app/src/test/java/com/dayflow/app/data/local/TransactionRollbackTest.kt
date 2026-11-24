package com.dayflow.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.dao.TaskDao
import com.dayflow.app.data.local.entity.TaskEntity
import com.dayflow.app.data.local.entity.TaskSubtaskEntity
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TransactionRollbackTest {

    private lateinit var database: DayflowDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleTask(id: String): TaskEntity {
        val now = Instant.now()
        return TaskEntity(
            id = id,
            title = "Atomic Task $id",
            description = null,
            categoryId = null,
            priority = Priority.MEDIUM,
            deadlineDate = LocalDate.of(2026, 11, 20),
            deadlineTime = LocalTime.of(12, 0),
            estimatedDurationMinutes = 60,
            plannedDate = null,
            plannedStart = null,
            plannedEnd = null,
            status = TaskStatus.PENDING,
            shouldCarryForward = true,
            createdAt = now,
            completedAt = null,
            updatedAt = now
        )
    }

    @Test
    fun transactionRollback_whenExceptionThrown_revertsAllOperations() = runBlocking {
        val task1 = sampleTask("tx_task_1")
        val task2 = sampleTask("tx_task_2")

        var caughtException = false
        try {
            database.withTransaction {
                taskDao.insertTask(task1)
                // Simulate an intentional failure during the transaction
                throw IllegalStateException("Simulated mid-transaction failure")
            }
        } catch (e: IllegalStateException) {
            caughtException = true
        }

        assertTrue("Expected IllegalStateException to be caught", caughtException)
        // Verify task1 was rolled back and is NOT present in the database
        assertNull(taskDao.getTaskById("tx_task_1").first())
        val allTasks = taskDao.getTasksByStatus(TaskStatus.PENDING).first()
        assertEquals(0, allTasks.size)
    }

    @Test
    fun atomicSuccess_commitsAllOperations() = runBlocking {
        val task = sampleTask("tx_task_parent")
        val sub1 = TaskSubtaskEntity("tx_sub_1", task.id, "Step 1", false, 0, Instant.now())
        val sub2 = TaskSubtaskEntity("tx_sub_2", task.id, "Step 2", false, 1, Instant.now())

        taskDao.insertTaskWithSubtasks(task, listOf(sub1, sub2))

        val loadedTask = taskDao.getTaskById(task.id).first()
        val loadedSubtasks = taskDao.getSubtasksForTask(task.id).first()

        assertEquals("tx_task_parent", loadedTask?.id)
        assertEquals(2, loadedSubtasks.size)
    }
}
