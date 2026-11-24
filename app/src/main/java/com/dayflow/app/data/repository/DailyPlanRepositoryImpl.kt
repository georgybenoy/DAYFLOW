package com.dayflow.app.data.repository

import com.dayflow.app.core.common.IoDispatcher
import com.dayflow.app.data.local.dao.DailyPlanDao
import com.dayflow.app.data.local.dao.DailyReviewDao
import com.dayflow.app.data.mapper.toDomain
import com.dayflow.app.data.mapper.toEntity
import com.dayflow.app.domain.model.DailyPlan
import com.dayflow.app.domain.model.DailyPlanItem
import com.dayflow.app.domain.model.DailyReview
import com.dayflow.app.domain.repository.DailyPlanRepository
import com.dayflow.app.domain.repository.DailyReviewRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyPlanRepositoryImpl @Inject constructor(
    private val dailyPlanDao: DailyPlanDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DailyPlanRepository {

    override fun getPlanByDate(date: LocalDate): Flow<DailyPlan?> {
        return dailyPlanDao.getPlanByDate(date).flatMapLatest { planEntity ->
            if (planEntity == null) {
                flowOf(null)
            } else {
                dailyPlanDao.getItemsForPlan(planEntity.id).map { items ->
                    planEntity.toDomain(items)
                }
            }
        }
    }

    override suspend fun getPlanByDateDirect(date: LocalDate): DailyPlan? = withContext(ioDispatcher) {
        val entity = dailyPlanDao.getPlanByDateDirect(date) ?: return@withContext null
        val items = dailyPlanDao.getItemsForPlanDirect(entity.id)
        entity.toDomain(items)
    }

    override fun getPlansBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyPlan>> {
        return dailyPlanDao.getPlansBetween(startDate, endDate).map { list ->
            list.map { entity ->
                val items = dailyPlanDao.getItemsForPlanDirect(entity.id)
                entity.toDomain(items)
            }
        }
    }

    override suspend fun getPlansBetweenDirect(startDate: LocalDate, endDate: LocalDate): List<DailyPlan> = withContext(ioDispatcher) {
        val plans = dailyPlanDao.getPlansBetweenDirect(startDate, endDate)
        plans.map { entity ->
            val items = dailyPlanDao.getItemsForPlanDirect(entity.id)
            entity.toDomain(items)
        }
    }

    override fun getPlanItems(planId: String): Flow<List<DailyPlanItem>> {
        return dailyPlanDao.getItemsForPlan(planId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun savePlanWithItems(plan: DailyPlan, items: List<DailyPlanItem>) = withContext(ioDispatcher) {
        dailyPlanDao.savePlanWithItems(plan.toEntity(), items.map { it.toEntity() })
    }

    override suspend fun updatePlanItem(item: DailyPlanItem) = withContext(ioDispatcher) {
        dailyPlanDao.updatePlanItem(item.toEntity())
    }

    override suspend fun deletePlan(plan: DailyPlan) = withContext(ioDispatcher) {
        dailyPlanDao.deletePlan(plan.toEntity())
    }
}

@Singleton
class DailyReviewRepositoryImpl @Inject constructor(
    private val dailyReviewDao: DailyReviewDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DailyReviewRepository {

    override fun getReviewByDate(date: LocalDate): Flow<DailyReview?> {
        return dailyReviewDao.getReviewByDate(date).map { it?.toDomain() }
    }

    override suspend fun getReviewByDateDirect(date: LocalDate): DailyReview? = withContext(ioDispatcher) {
        dailyReviewDao.getReviewByDateDirect(date)?.toDomain()
    }

    override fun getAllReviews(): Flow<List<DailyReview>> {
        return dailyReviewDao.getAllReviews().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveReview(review: DailyReview) = withContext(ioDispatcher) {
        dailyReviewDao.insertReview(review.toEntity())
        Unit
    }
}
