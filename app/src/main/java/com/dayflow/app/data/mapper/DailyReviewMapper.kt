package com.dayflow.app.data.mapper

import com.dayflow.app.data.local.entity.DailyReviewEntity
import com.dayflow.app.domain.model.DailyReview
import com.dayflow.app.domain.model.TaskReviewDecision
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun DailyReviewEntity.toDomain(): DailyReview {
    val decisionsList: List<TaskReviewDecision> = try {
        if (decisionsJson.isNotBlank()) {
            json.decodeFromString(decisionsJson)
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }

    return DailyReview(
        id = id,
        localDate = localDate,
        notes = notes,
        reviewedAt = reviewedAt,
        tasksCompletedCount = tasksCompletedCount,
        tasksUnfinishedCount = tasksUnfinishedCount,
        habitsCompletedCount = habitsCompletedCount,
        decisions = decisionsList
    )
}

fun DailyReview.toEntity(): DailyReviewEntity {
    val jsonString = try {
        json.encodeToString(decisions)
    } catch (e: Exception) {
        "[]"
    }

    return DailyReviewEntity(
        id = id,
        localDate = localDate,
        notes = notes,
        reviewedAt = reviewedAt,
        tasksCompletedCount = tasksCompletedCount,
        tasksUnfinishedCount = tasksUnfinishedCount,
        habitsCompletedCount = habitsCompletedCount,
        decisionsJson = jsonString
    )
}
