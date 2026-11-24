package com.dayflow.app.feature.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.calendar.CalendarEvent
import com.dayflow.app.domain.calendar.CalendarEventType
import com.dayflow.app.domain.model.WeekStartDay
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class CalendarScreenComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDate = LocalDate.of(2026, 11, 19)

    private val sampleEvents = listOf(
        CalendarEvent(
            id = "t_1",
            title = "Design System Review",
            type = CalendarEventType.TASK,
            date = testDate,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            referenceId = "task_ref_1"
        ),
        CalendarEvent(
            id = "r_1",
            title = "Hydration Reminder",
            type = CalendarEventType.REMINDER,
            date = testDate,
            startTime = LocalTime.of(12, 0),
            referenceId = "rem_ref_1"
        )
    )

    @Test
    fun calendarContent_dayMode_rendersTimelineAndHeader() {
        var clickedEvent: CalendarEvent? = null
        var changedMode: CalendarViewMode? = null

        composeTestRule.setContent {
            DayflowTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        selectedDate = testDate,
                        viewMode = CalendarViewMode.DAY,
                        events = sampleEvents,
                        weekStartDay = WeekStartDay.MONDAY,
                        isLoading = false
                    ),
                    onViewModeChanged = { changedMode = it },
                    onDateSelected = {},
                    onFilterSelected = {},
                    onNavigatePrevious = {},
                    onNavigateNext = {},
                    onGoToToday = {},
                    onEventClick = { clickedEvent = it },
                    onNavigateBack = {}
                )
            }
        }

        // Title and controls
        composeTestRule.onNodeWithText("Calendar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today").assertIsDisplayed()
        composeTestRule.onNodeWithText("Day").assertIsDisplayed()
        composeTestRule.onNodeWithText("Week").assertIsDisplayed()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()

        // Events rendered in timeline
        composeTestRule.onNodeWithText("Design System Review").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hydration Reminder").assertIsDisplayed()

        // Click event triggers onEventClick
        composeTestRule.onNodeWithText("Design System Review").performClick()
        assertEquals("task_ref_1", clickedEvent?.referenceId)

        // Switch to Week mode
        composeTestRule.onNodeWithText("Week").performClick()
        assertEquals(CalendarViewMode.WEEK, changedMode)
    }

    @Test
    fun calendarContent_emptyEvents_rendersEmptyTimelineState() {
        composeTestRule.setContent {
            DayflowTheme {
                CalendarContent(
                    uiState = CalendarUiState(
                        selectedDate = testDate,
                        viewMode = CalendarViewMode.DAY,
                        events = emptyList(),
                        weekStartDay = WeekStartDay.MONDAY,
                        isLoading = false
                    ),
                    onViewModeChanged = {},
                    onDateSelected = {},
                    onFilterSelected = {},
                    onNavigatePrevious = {},
                    onNavigateNext = {},
                    onGoToToday = {},
                    onEventClick = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No commitments scheduled").assertIsDisplayed()
    }
}
