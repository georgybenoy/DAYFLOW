package com.dayflow.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dayflow.app.data.local.DayflowDatabase
import com.dayflow.app.data.local.entity.AppSettingEntity
import com.dayflow.app.data.local.entity.DailyReviewEntity
import com.dayflow.app.data.mapper.toDomain
import com.dayflow.app.data.mapper.toEntity
import com.dayflow.app.domain.model.DailyReview
import com.dayflow.app.domain.model.ReviewDecisionType
import com.dayflow.app.domain.model.TaskReviewDecision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class DailyReviewDaoTest {

    private lateinit var database: DayflowDatabase
    private lateinit var reviewDao: DailyReviewDao
    private lateinit var appSettingDao: AppSettingDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DayflowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reviewDao = database.dailyReviewDao()
        appSettingDao = database.appSettingDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sampleDomainReview(date: LocalDate = LocalDate.of(2026, 11, 18)): DailyReview {
        return DailyReview(
            id = "review_${date}",
            localDate = date,
            notes = "Productive day. Finished core DB schema.",
            reviewedAt = Instant.now(),
            tasksCompletedCount = 5,
            tasksUnfinishedCount = 2,
            habitsCompletedCount = 3,
            decisions = listOf(
                TaskReviewDecision(
                    taskId = "task_reschedule",
                    taskTitle = "Write final report",
                    decision = ReviewDecisionType.RESCHEDULED,
                    rescheduledToDate = "2026-11-19"
                ),
                TaskReviewDecision(
                    taskId = "task_drop",
                    taskTitle = "Deprecated research",
                    decision = ReviewDecisionType.CANCELLED,
                    rescheduledToDate = null
                )
            )
        )
    }

    @Test
    fun dailyReviewCrud_andSerializationRoundtrip() = runBlocking {
        val domainReview = sampleDomainReview()
        val entity = domainReview.toEntity()

        reviewDao.insertReview(entity)

        val retrievedEntity = reviewDao.getReviewByDate(domainReview.localDate).first()
        assertNotNull(retrievedEntity)
        assertEquals(domainReview.id, retrievedEntity?.id)
        assertEquals(domainReview.tasksCompletedCount, retrievedEntity?.tasksCompletedCount)
        assertEquals(domainReview.tasksUnfinishedCount, retrievedEntity?.tasksUnfinishedCount)

        // Convert back to domain and verify decisions
        val roundtripDomain = retrievedEntity!!.toDomain()
        assertEquals(domainReview.id, roundtripDomain.id)
        assertEquals(domainReview.localDate, roundtripDomain.localDate)
        assertEquals(domainReview.notes, roundtripDomain.notes)
        assertEquals(2, roundtripDomain.decisions.size)

        val decision1 = roundtripDomain.decisions[0]
        assertEquals("task_reschedule", decision1.taskId)
        assertEquals("Write final report", decision1.taskTitle)
        assertEquals(ReviewDecisionType.RESCHEDULED, decision1.decision)
        assertEquals("2026-11-19", decision1.rescheduledToDate)

        val decision2 = roundtripDomain.decisions[1]
        assertEquals("task_drop", decision2.taskId)
        assertEquals("Deprecated research", decision2.taskTitle)
        assertEquals(ReviewDecisionType.CANCELLED, decision2.decision)
        assertNull(decision2.rescheduledToDate)
    }

    @Test
    fun getAllReviews_ordersByLocalDateDescending() = runBlocking {
        val review1 = sampleDomainReview(LocalDate.of(2026, 11, 16)).toEntity()
        val review2 = sampleDomainReview(LocalDate.of(2026, 11, 18)).toEntity()
        val review3 = sampleDomainReview(LocalDate.of(2026, 11, 17)).toEntity()

        reviewDao.insertReview(review1)
        reviewDao.insertReview(review2)
        reviewDao.insertReview(review3)

        val allReviews = reviewDao.getAllReviews().first()
        assertEquals(3, allReviews.size)
        assertEquals(LocalDate.of(2026, 11, 18), allReviews[0].localDate)
        assertEquals(LocalDate.of(2026, 11, 17), allReviews[1].localDate)
        assertEquals(LocalDate.of(2026, 11, 16), allReviews[2].localDate)
    }

    @Test
    fun appSettingDao_insertGetDelete() = runBlocking {
        val setting = AppSettingEntity("onboarding_complete", "true", Instant.now())
        appSettingDao.insertOrUpdateSetting(setting)

        val retrieved = appSettingDao.getSetting("onboarding_complete").first()
        assertNotNull(retrieved)
        assertEquals("true", retrieved?.value)

        val direct = appSettingDao.getSettingDirect("onboarding_complete")
        assertNotNull(direct)
        assertEquals("true", direct?.value)

        // Update value
        appSettingDao.insertOrUpdateSetting(setting.copy(value = "false"))
        assertEquals("false", appSettingDao.getSettingDirect("onboarding_complete")?.value)

        // Delete
        appSettingDao.deleteSetting("onboarding_complete")
        assertNull(appSettingDao.getSettingDirect("onboarding_complete"))
    }
}
