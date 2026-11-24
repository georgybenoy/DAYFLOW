package com.dayflow.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.entity.TaskEntity
import com.dayflow.app.data.local.entity.TaskSubtaskEntity
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class TaskDaoTest {

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

    private fun sampleTask(id: String = "task_project"): TaskEntity {
        return TaskEntity(
            id = id,
            title = "Final Year Report",
            description = "Write documentation and architecture",
            categoryId = null,
            priority = Priority.HIGH,
            deadlineDate = LocalDate.of(2026, 11, 20),
            deadlineTime = LocalTime.of(17, 0),
            estimatedDurationMinutes = 180,
            plannedDate = LocalDate.of(2026, 11, 19),
            plannedStart = LocalTime.of(9, 0),
            plannedEnd = LocalTime.of(12, 0),
            status = TaskStatus.PENDING,
            shouldCarryForward = true,
            createdAt = Instant.now(),
            completedAt = null,
            updatedAt = Instant.now()
        )
    }

    @Test
    fun taskCrud_andStatusFiltering() = runBlocking {
        val task = sampleTask()
        taskDao.insertTask(task)

        val retrieved = taskDao.getTaskById("task_project").first()
        assertNotNull(retrieved)
        assertEquals("Final Year Report", retrieved?.title)
        assertEquals(Priority.HIGH, retrieved?.priority)

        val pending = taskDao.getTasksByStatus(TaskStatus.PENDING).first()
        assertEquals(1, pending.size)

        val completed = taskDao.getTasksByStatus(TaskStatus.COMPLETED).first()
        assertEquals(0, completed.size)

        // Complete the task
        taskDao.updateTask(task.copy(status = TaskStatus.COMPLETED, completedAt = Instant.now()))
        assertEquals(1, taskDao.getTasksByStatus(TaskStatus.COMPLETED).first().size)
    }

    @Test
    fun taskSubtasks_cascadeDeleteAndOrdering() = runBlocking {
        val task = sampleTask("task_parent")
        val sub1 = TaskSubtaskEntity("sub_1", "task_parent", "Draft architecture", false, 0, Instant.now())
        val sub2 = TaskSubtaskEntity("sub_2", "task_parent", "Write database doc", false, 1, Instant.now())

        taskDao.insertTaskWithSubtasks(task, listOf(sub1, sub2))

        val subtasks = taskDao.getSubtasksForTask("task_parent").first()
        assertEquals(2, subtasks.size)
        assertEquals("sub_1", subtasks[0].id)
        assertEquals("sub_2", subtasks[1].id)

        // Delete parent task
        taskDao.deleteTaskById("task_parent")

        // Subtasks must be cascade deleted
        val remainingSubtasks = taskDao.getSubtasksForTask("task_parent").first()
        assertEquals(0, remainingSubtasks.size)
    }

    @Test
    fun derivedOverdueStatus_evaluatesCorrectlyWithoutStoredColumn() {
        val today = LocalDate.of(2026, 11, 18)
        val now = LocalTime.of(14, 0)

        // Past deadline -> Overdue
        val pastTask = Task(
            id = "t1",
            title = "Overdue task",
            deadlineDate = LocalDate.of(2026, 11, 17),
            deadlineTime = LocalTime.of(10, 0),
            status = TaskStatus.PENDING
        )
        assertTrue(pastTask.isOverdue(today, now))

        // Same day earlier time -> Overdue
        val earlierTodayTask = Task(
            id = "t2",
            title = "Earlier today",
            deadlineDate = today,
            deadlineTime = LocalTime.of(12, 0),
            status = TaskStatus.PENDING
        )
        assertTrue(earlierTodayTask.isOverdue(today, now))

        // Same day later time -> Not overdue
        val laterTodayTask = Task(
            id = "t3",
            title = "Later today",
            deadlineDate = today,
            deadlineTime = LocalTime.of(18, 0),
            status = TaskStatus.PENDING
        )
        assertFalse(laterTodayTask.isOverdue(today, now))

        // Future day -> Not overdue
        val futureTask = Task(
            id = "t4",
            title = "Future task",
            deadlineDate = LocalDate.of(2026, 11, 25),
            deadlineTime = LocalTime.of(12, 0),
            status = TaskStatus.PENDING
        )
        assertFalse(futureTask.isOverdue(today, now))

        // Completed task is never overdue
        val completedPastTask = pastTask.copy(status = TaskStatus.COMPLETED)
        assertFalse(completedPastTask.isOverdue(today, now))
    }
}
