package com.dayflow.app.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.dayflow.app.domain.model.ThemePreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dataStoreScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var testFile: File
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        testFile = tempFolder.newFile("test_preferences.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { testFile }
        )
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun defaultSettings_matchExpectedDefaults() = runTest {
        val profile = repository.userProfile.first()

        assertEquals("", profile.name)
        assertEquals(ThemePreference.DARK, profile.themePreference)
        assertEquals(LocalTime.of(7, 0), profile.morningBriefingTime)
        assertEquals(LocalTime.of(21, 30), profile.eveningReviewTime)
        assertEquals(LocalTime.of(22, 0), profile.quietHoursStart)
        assertEquals(LocalTime.of(7, 0), profile.quietHoursEnd)
        assertFalse(profile.isOnboardingCompleted)
        assertFalse(profile.exactAlarmAcknowledged)
    }

    @Test
    fun setThemePreference_persistsAndUpdatesFlow() = runTest {
        repository.setThemePreference(ThemePreference.LIGHT)
        assertEquals(ThemePreference.LIGHT, repository.themePreference.first())

        repository.setThemePreference(ThemePreference.SYSTEM)
        assertEquals(ThemePreference.SYSTEM, repository.themePreference.first())

        repository.setThemePreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, repository.themePreference.first())
    }

    @Test
    fun completeOnboarding_persistsAllValues() = runTest {
        val briefing = LocalTime.of(6, 30)
        val review = LocalTime.of(22, 0)
        val quietStart = LocalTime.of(23, 0)
        val quietEnd = LocalTime.of(6, 0)

        repository.completeOnboarding(
            name = "Jordan",
            theme = ThemePreference.LIGHT,
            briefingTime = briefing,
            reviewTime = review,
            quietStart = quietStart,
            quietEnd = quietEnd
        )

        val profile = repository.userProfile.first()
        assertEquals("Jordan", profile.name)
        assertEquals(ThemePreference.LIGHT, profile.themePreference)
        assertEquals(briefing, profile.morningBriefingTime)
        assertEquals(review, profile.eveningReviewTime)
        assertEquals(quietStart, profile.quietHoursStart)
        assertEquals(quietEnd, profile.quietHoursEnd)
        assertTrue(profile.isOnboardingCompleted)
    }

    @Test
    fun skipOnboarding_marksOnboardingComplete() = runTest {
        assertFalse(repository.isOnboardingCompleted.first())

        repository.skipOnboarding()

        assertTrue(repository.isOnboardingCompleted.first())
    }
}
