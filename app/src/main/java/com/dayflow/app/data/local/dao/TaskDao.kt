package com.dayflow.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dayflow.app.data.local.entity.TaskEntity
import com.dayflow.app.data.local.entity.TaskSubtaskEntity
import com.dayflow.app.domain.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getTaskById(id: String): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskByIdDirect(id: String): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priority ASC, deadlineDate ASC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE plannedDate = :date ORDER BY plannedStart ASC, priority ASC")
    fun getTasksByPlannedDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deadlineDate = :date ORDER BY deadlineTime ASC, priority ASC")
    fun getTasksByDeadlineDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE status = 'PENDING' AND shouldCarryForward = 1")
    fun getEligibleCarryForwardTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (plannedDate BETWEEN :startDate AND :endDate) OR (deadlineDate BETWEEN :startDate AND :endDate) ORDER BY priority ASC")
    fun getTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE (plannedDate BETWEEN :startDate AND :endDate) OR (deadlineDate BETWEEN :startDate AND :endDate) ORDER BY priority ASC")
    suspend fun getTasksBetweenDirect(startDate: LocalDate, endDate: LocalDate): List<TaskEntity>

    // Subtasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: TaskSubtaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<TaskSubtaskEntity>): List<Long>

    @Update
    suspend fun updateSubtask(subtask: TaskSubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: TaskSubtaskEntity)

    @Query("DELETE FROM task_subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: String)

    @Query("SELECT * FROM task_subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC")
    fun getSubtasksForTask(taskId: String): Flow<List<TaskSubtaskEntity>>

    @Query("SELECT * FROM task_subtasks WHERE taskId = :taskId ORDER BY orderIndex ASC")
    suspend fun getSubtasksForTaskDirect(taskId: String): List<TaskSubtaskEntity>

    @Query("SELECT * FROM task_subtasks WHERE id = :id LIMIT 1")
    suspend fun getSubtaskById(id: String): TaskSubtaskEntity?

    @Query("DELETE FROM task_subtasks WHERE id = :id")
    suspend fun deleteSubtaskById(id: String)

    @Transaction
    suspend fun insertTaskWithSubtasks(task: TaskEntity, subtasks: List<TaskSubtaskEntity>) {
        insertTask(task)
        if (subtasks.isNotEmpty()) {
            insertSubtasks(subtasks)
        }
    }

    @Transaction
    suspend fun updateTaskWithSubtasks(task: TaskEntity, subtasks: List<TaskSubtaskEntity>) {
        insertTask(task)
        deleteSubtasksByTaskId(task.id)
        if (subtasks.isNotEmpty()) {
            insertSubtasks(subtasks)
        }
    }
}
