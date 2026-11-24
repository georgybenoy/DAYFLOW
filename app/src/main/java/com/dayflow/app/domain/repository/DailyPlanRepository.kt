package com.dayflow.app.domain.repository

import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DailyPlanRepository {
    fun getPlanByDate(date: LocalDate): Flow<DailyPlan?>
    suspend fun getPlanByDateDirect(date: LocalDate): DailyPlan?
    fun getPlansBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyPlan>>
    suspend fun getPlansBetweenDirect(startDate: LocalDate, endDate: LocalDate): List<DailyPlan>
    fun getPlanItems(planId: String): Flow<List<DailyPlanItem>>
    suspend fun savePlanWithItems(plan: DailyPlan, items: List<DailyPlanItem>)
    suspend fun updatePlanItem(item: DailyPlanItem)
    suspend fun deletePlan(plan: DailyPlan)
}

interface DailyReviewRepository {
    fun getReviewByDate(date: LocalDate): Flow<com.dayflow.app.domain.model.DailyReview?>
    suspend fun getReviewByDateDirect(date: LocalDate): com.dayflow.app.domain.model.DailyReview?
    fun getAllReviews(): Flow<List<com.dayflow.app.domain.model.DailyReview>>
    suspend fun saveReview(review: com.dayflow.app.domain.model.DailyReview)
}
