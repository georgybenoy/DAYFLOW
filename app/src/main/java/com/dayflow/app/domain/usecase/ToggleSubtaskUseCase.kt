package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleSubtaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Toggles completion status of a subtask.
     * Invariant: Subtask completion is tracked separately and does not complete the parent task.
     */
    suspend operator fun invoke(subtaskId: String, isCompleted: Boolean): Boolean {
        val subtask = taskRepository.getSubtaskById(subtaskId) ?: return false
        taskRepository.updateSubtask(subtask.copy(isCompleted = isCompleted))
        return true
    }
}
