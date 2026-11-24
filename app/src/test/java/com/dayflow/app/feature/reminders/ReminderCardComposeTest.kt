package com.dayflow.app.feature.reminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.feature.reminders.components.ReminderCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class ReminderCardComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun reminderCard_rendersTitleRecurrenceAndBadges() {
        val reminder = Reminder(
            id = "rem-card-test",
            title = "Hydration Break",
            description = "Drink 250ml water",
            type = ReminderType.DAILY,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(10, 0),
            priority = Priority.URGENT,
            precision = ReminderPrecision.EXACT
        )

        val item = ReminderListItem(
            reminder = reminder,
            recurrenceSummary = "Daily at 10:00 AM",
            nextDeliverySummary = "Today at 10:00 AM",
            nextDeliveryInstant = Instant.now()
        )

        var clicked = false
        var editClicked = false
        var pauseClicked = false
        var deleteClicked = false

        composeTestRule.setContent {
            DayflowTheme {
                ReminderCard(
                    item = item,
                    onClick = { clicked = true },
                    onEditClick = { editClicked = true },
                    onTogglePauseClick = { pauseClicked = true },
                    onDeleteClick = { deleteClicked = true }
                )
            }
        }

        // Verify title & details rendered
        composeTestRule.onNodeWithText("Hydration Break").assertIsDisplayed()
        composeTestRule.onNodeWithText("Drink 250ml water").assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily at 10:00 AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("Today at 10:00 AM").assertIsDisplayed()
        composeTestRule.onNodeWithText("URGENT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exact").assertIsDisplayed()

        // Verify click on card
        composeTestRule.onNodeWithText("Hydration Break").performClick()
        assertTrue("Card click should invoke onClick", clicked)

        // Verify overflow menu
        composeTestRule.onNodeWithContentDescription("Options for Hydration Break").performClick()
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()

        composeTestRule.onNodeWithText("Edit").performClick()
        assertTrue("Edit click should invoke onEditClick", editClicked)
    }
}
