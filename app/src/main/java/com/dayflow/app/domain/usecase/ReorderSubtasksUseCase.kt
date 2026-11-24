package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.TaskRepository
import javax.inject.Inject

class ReorderSubtasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: String, reorderedSubtaskIds: List<String>) {
        val existing = taskRepository.getSubtasksForTaskDirect(taskId).associateBy { it.id }
        reorderedSubtaskIds.forEachIndexed { index, id ->
            existing[id]?.let { subtask ->
                if (subtask.orderIndex != index) {
                    taskRepository.updateSubtask(subtask.copy(orderIndex = index))
                }
            }
        }
    }
}
