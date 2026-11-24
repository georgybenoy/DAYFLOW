package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.model.TaskSubtask
import com.dayflow.app.domain.repository.TaskRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class AddSubtaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(
        taskId: String,
        title: String,
        orderIndex: Int? = null,
        now: Instant = Instant.now()
    ): TaskSubtask {
        val existing = taskRepository.getSubtasksForTaskDirect(taskId)
        val index = orderIndex ?: existing.size
        val subtask = TaskSubtask(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            title = title.trim(),
            isCompleted = false,
            orderIndex = index,
            createdAt = now
        )
        taskRepository.saveSubtask(subtask)
        return subtask
    }
}
