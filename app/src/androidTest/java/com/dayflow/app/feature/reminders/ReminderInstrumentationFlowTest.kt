package com.dayflow.app.feature.reminders

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayflow.app.core.designsystem.DayflowTheme
import com.dayflow.app.domain.model.Priority
import com.dayflow.app.domain.model.Reminder
import com.dayflow.app.domain.model.ReminderPrecision
import com.dayflow.app.domain.model.ReminderType
import com.dayflow.app.feature.reminders.components.ReminderCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class ReminderInstrumentationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun waterReminderCard_displaysFixedIntervalCadence() {
        val waterReminder = Reminder(
            id = "rem-water-inst",
            title = "Drink Water (Every 2h)",
            description = "Stay fresh and focused",
            type = ReminderType.FIXED_INTERVAL,
            startDate = LocalDate.now(),
            startTime = LocalTime.of(8, 0),
            endTime = LocalTime.of(22, 0),
            intervalMinutes = 120,
            priority = Priority.HIGH,
            precision = ReminderPrecision.EXACT
        )

        val item = ReminderListItem(
            reminder = waterReminder,
            recurrenceSummary = "Every 2 hours (08:00 - 22:00)",
            nextDeliverySummary = "Today at 08:00 AM",
            nextDeliveryInstant = Instant.now()
        )

        composeTestRule.setContent {
            DayflowTheme {
                ReminderCard(
                    item = item,
                    onClick = {},
                    onEditClick = {},
                    onTogglePauseClick = {},
                    onDeleteClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Drink Water (Every 2h)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Every 2 hours (08:00 - 22:00)").assertIsDisplayed()
        composeTestRule.onNodeWithText("HIGH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exact").assertIsDisplayed()
    }
}
