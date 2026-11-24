package com.dayflow.app.domain.repository

import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>
    fun getTasksByPlannedDate(date: LocalDate): Flow<List<Task>>
    fun getTasksByDeadlineDate(date: LocalDate): Flow<List<Task>>
    fun getTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>>
    suspend fun getTasksBetweenDirect(startDate: LocalDate, endDate: LocalDate): List<Task>
    fun getEligibleCarryForwardTasks(): Flow<List<Task>>
    fun getTaskById(id: String): Flow<Task?>
    suspend fun getTaskByIdDirect(id: String): Task?

    suspend fun saveTask(task: Task)
    suspend fun saveTaskWithSubtasks(task: Task, subtasks: List<TaskSubtask>)
    suspend fun updateTaskWithSubtasks(task: Task, subtasks: List<TaskSubtask>)
    suspend fun deleteTask(task: Task)
    suspend fun deleteTaskById(id: String)

    suspend fun getSubtasksForTaskDirect(taskId: String): List<TaskSubtask>
    suspend fun getSubtaskById(id: String): TaskSubtask?
    suspend fun saveSubtask(subtask: TaskSubtask)
    suspend fun updateSubtask(subtask: TaskSubtask)
    suspend fun deleteSubtask(subtask: TaskSubtask)
    suspend fun deleteSubtaskById(id: String)
}
