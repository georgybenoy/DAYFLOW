package com.dayflow.app.data.repository

import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.data.local.dao.TaskDao
import com.dayflow.app.data.mapper.toDomain
import com.dayflow.app.data.mapper.toEntity
import com.dayflow.app.domain.model.Task
import com.dayflow.app.domain.model.TaskStatus
import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TaskRepository {

    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> {
        return taskDao.getTasksByStatus(status).map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksByPlannedDate(date: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksByPlannedDate(date).map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksByDeadlineDate(date: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksByDeadlineDate(date).map { list -> list.map { it.toDomain() } }
    }

    override fun getTasksBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<Task>> {
        return taskDao.getTasksBetween(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTasksBetweenDirect(startDate: LocalDate, endDate: LocalDate): List<Task> {
        return taskDao.getTasksBetweenDirect(startDate, endDate).map { it.toDomain() }
    }

    override fun getEligibleCarryForwardTasks(): Flow<List<Task>> {
        return taskDao.getEligibleCarryForwardTasks().map { list -> list.map { it.toDomain() } }
    }

    override fun getTaskById(id: String): Flow<Task?> {
        return taskDao.getTaskById(id).flatMapLatest { taskEntity ->
            if (taskEntity == null) {
                flowOf(null)
            } else {
                taskDao.getSubtasksForTask(id).map { subtasks ->
                    taskEntity.toDomain(subtasks)
                }
            }
        }
    }

    override suspend fun getTaskByIdDirect(id: String): Task? = withContext(ioDispatcher) {
        val entity = taskDao.getTaskByIdDirect(id) ?: return@withContext null
        val subtasks = taskDao.getSubtasksForTaskDirect(id)
        entity.toDomain(subtasks)
    }

    override suspend fun saveTask(task: Task) = withContext(ioDispatcher) {
        taskDao.insertTask(task.toEntity())
        Unit
    }

    override suspend fun saveTaskWithSubtasks(task: Task, subtasks: List<TaskSubtask>) = withContext(ioDispatcher) {
        taskDao.insertTaskWithSubtasks(task.toEntity(), subtasks.map { it.toEntity() })
    }

    override suspend fun updateTaskWithSubtasks(task: Task, subtasks: List<TaskSubtask>) = withContext(ioDispatcher) {
        taskDao.updateTaskWithSubtasks(task.toEntity(), subtasks.map { it.toEntity() })
    }

    override suspend fun deleteTask(task: Task) = withContext(ioDispatcher) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun deleteTaskById(id: String) = withContext(ioDispatcher) {
        taskDao.deleteTaskById(id)
    }

    override suspend fun getSubtasksForTaskDirect(taskId: String): List<TaskSubtask> = withContext(ioDispatcher) {
        taskDao.getSubtasksForTaskDirect(taskId).map { it.toDomain() }
    }

    override suspend fun getSubtaskById(id: String): TaskSubtask? = withContext(ioDispatcher) {
        taskDao.getSubtaskById(id)?.toDomain()
    }

    override suspend fun saveSubtask(subtask: TaskSubtask) = withContext(ioDispatcher) {
        taskDao.insertSubtask(subtask.toEntity())
        Unit
    }

    override suspend fun updateSubtask(subtask: TaskSubtask) = withContext(ioDispatcher) {
        taskDao.updateSubtask(subtask.toEntity())
    }

    override suspend fun deleteSubtask(subtask: TaskSubtask) = withContext(ioDispatcher) {
        taskDao.deleteSubtask(subtask.toEntity())
    }

    override suspend fun deleteSubtaskById(id: String) = withContext(ioDispatcher) {
        taskDao.deleteSubtaskById(id)
    }
}
