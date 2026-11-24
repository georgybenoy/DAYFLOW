package com.dayflow.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.entity.DailyPlanEntity
import com.dayflow.app.data.local.entity.DailyPlanItemEntity
import com.dayflow.app.domain.model.PlanItemSourceType
import com.dayflow.app.domain.model.PlanStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class DailyPlanDaoTest {

    private lateinit var database: DayflowDatabase
    private lateinit var planDao: DailyPlanDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        planDao = database.dailyPlanDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun samplePlan(
        id: String = "plan_20261118",
        date: LocalDate = LocalDate.of(2026, 11, 18),
        status: PlanStatus = PlanStatus.DRAFT
    ): DailyPlanEntity {
        val now = Instant.now()
        return DailyPlanEntity(
            id = id,
            localDate = date,
            status = status,
            createdAt = now,
            approvedAt = null,
            updatedAt = now
        )
    }

    private fun samplePlanItems(planId: String): List<DailyPlanItemEntity> {
        return listOf(
            DailyPlanItemEntity(
                id = "item_1",
                planId = planId,
                sourceType = PlanItemSourceType.HABIT,
                sourceId = "habit_run",
                title = "Morning 5k Run",
                startTime = LocalTime.of(6, 30),
                endTime = LocalTime.of(7, 15),
                orderIndex = 0,
                isCompleted = true
            ),
            DailyPlanItemEntity(
                id = "item_2",
                planId = planId,
                sourceType = PlanItemSourceType.TASK,
                sourceId = "task_report",
                title = "Draft System Architecture",
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(11, 30),
                orderIndex = 1,
                isCompleted = false
            ),
            DailyPlanItemEntity(
                id = "item_3",
                planId = planId,
                sourceType = PlanItemSourceType.CUSTOM,
                sourceId = null,
                title = "Team Standup & Sync",
                startTime = LocalTime.of(12, 0),
                endTime = LocalTime.of(12, 30),
                orderIndex = 2,
                isCompleted = false
            )
        )
    }

    @Test
    fun planCrud_andDateLookup() = runBlocking {
        val targetDate = LocalDate.of(2026, 11, 18)
        val plan = samplePlan(date = targetDate, status = PlanStatus.DRAFT)
        planDao.insertPlan(plan)

        val retrievedFlow = planDao.getPlanByDate(targetDate).first()
        assertNotNull(retrievedFlow)
        assertEquals(plan.id, retrievedFlow?.id)
        assertEquals(PlanStatus.DRAFT, retrievedFlow?.status)

        // Direct query
        val retrievedDirect = planDao.getPlanByDateDirect(targetDate)
        assertNotNull(retrievedDirect)
        assertEquals(plan.id, retrievedDirect?.id)

        // Approve plan
        val now = Instant.now()
        val approved = plan.copy(status = PlanStatus.APPROVED, approvedAt = now, updatedAt = now)
        planDao.updatePlan(approved)

        val updated = planDao.getPlanById(plan.id).first()
        assertEquals(PlanStatus.APPROVED, updated?.status)
        assertEquals(now.toEpochMilli(), updated?.approvedAt?.toEpochMilli())
    }

    @Test
    fun savePlanWithItems_insertsAndOrdersItemsCorrectly() = runBlocking {
        val plan = samplePlan()
        val items = samplePlanItems(plan.id)

        planDao.savePlanWithItems(plan, items)

        val loadedItems = planDao.getItemsForPlan(plan.id).first()
        assertEquals(3, loadedItems.size)
        // Ensure ascending orderIndex
        assertEquals(0, loadedItems[0].orderIndex)
        assertEquals(1, loadedItems[1].orderIndex)
        assertEquals(2, loadedItems[2].orderIndex)
        assertEquals("Morning 5k Run", loadedItems[0].title)
        assertTrue(loadedItems[0].isCompleted)
        assertEquals("Draft System Architecture", loadedItems[1].title)
    }

    @Test
    fun savePlanWithItems_replacesExistingItemsAtomically() = runBlocking {
        val plan = samplePlan()
        val initialItems = samplePlanItems(plan.id)
        planDao.savePlanWithItems(plan, initialItems)

        val initialLoaded = planDao.getItemsForPlanDirect(plan.id)
        assertEquals(3, initialLoaded.size)

        // Replace with new single item
        val newItems = listOf(
            DailyPlanItemEntity(
                id = "item_new_1",
                planId = plan.id,
                sourceType = PlanItemSourceType.TASK,
                sourceId = "task_99",
                title = "Updated Task Item",
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                orderIndex = 0,
                isCompleted = false
            )
        )
        planDao.savePlanWithItems(plan, newItems)

        val updatedLoaded = planDao.getItemsForPlanDirect(plan.id)
        assertEquals(1, updatedLoaded.size)
        assertEquals("item_new_1", updatedLoaded[0].id)
        assertEquals("Updated Task Item", updatedLoaded[0].title)
    }

    @Test
    fun cascadeDelete_deletingPlanDeletesAllItems() = runBlocking {
        val plan = samplePlan()
        val items = samplePlanItems(plan.id)
        planDao.savePlanWithItems(plan, items)

        assertEquals(3, planDao.getItemsForPlanDirect(plan.id).size)

        // Delete the parent plan
        planDao.deletePlan(plan)

        assertNull(planDao.getPlanByDateDirect(plan.localDate))
        val remainingItems = planDao.getItemsForPlanDirect(plan.id)
        assertEquals(0, remainingItems.size)
    }

    @Test
    fun updatePlanItem_updatesCompletionStatus() = runBlocking {
        val plan = samplePlan()
        val items = samplePlanItems(plan.id)
        planDao.savePlanWithItems(plan, items)

        val itemToUpdate = items[1].copy(isCompleted = true)
        planDao.updatePlanItem(itemToUpdate)

        val loaded = planDao.getItemsForPlanDirect(plan.id)
        val updatedItem = loaded.first { it.id == "item_2" }
        assertTrue(updatedItem.isCompleted)
    }
}
