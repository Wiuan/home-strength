package com.homestrength

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.seed.SeedData
import com.homestrength.ui.theme.HomeStrengthTheme
import com.homestrength.ui.workout.CompleteWorkoutScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Reproduces the previous crash: Complete screen Scaffold composing while session is still null.
 */
@RunWith(AndroidJUnit4::class)
class CompleteWorkoutScreenSmokeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HomeStrengthDatabase
    private lateinit var repository: HomeStrengthRepository
    private var sessionId: Long = 0L

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, HomeStrengthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.exerciseDao().insertAll(SeedData.defaultExercises())
        db.bandDao().insertAll(SeedData.defaultBands())
        repository = HomeStrengthRepository(
            exerciseDao = db.exerciseDao(),
            bandDao = db.bandDao(),
            settingsDao = db.appSettingsDao(),
            sessionDao = db.workoutSessionDao()
        )
        sessionId = repository.startWorkout(TrainingMode.NORMAL)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun completeScreen_loadsAndSavesWithoutCrash() {
        val done = CountDownLatch(1)
        val errored = AtomicBoolean(false)
        composeRule.setContent {
            HomeStrengthTheme {
                CompleteWorkoutScreen(
                    repository = repository,
                    sessionId = sessionId,
                    onDone = { done.countDown() },
                    onError = { errored.set(true) }
                )
            }
        }

        // Title appears in TopAppBar + body; either means composition succeeded (no SlotTable crash).
        composeRule.onAllNodesWithText("训练完成").onFirst().assertIsDisplayed()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("本次训练感觉")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText("4").performClick()
        composeRule.onNodeWithText("完成").performClick()

        assertTrue("onDone should be called", done.await(5, TimeUnit.SECONDS))
        assertTrue("should not error", !errored.get())
    }
}
