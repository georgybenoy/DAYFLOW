package com.dayflow.app.domain.usecase

import com.dayflow.app.domain.repository.TaskRepository
import javax.inject.Inject

class DeleteSubtaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(subtaskId: String) {
        taskRepository.deleteSubtaskById(subtaskId)
    }
}
