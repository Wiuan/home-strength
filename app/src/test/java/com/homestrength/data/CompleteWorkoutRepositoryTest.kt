package com.homestrength.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.seed.SeedData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CompleteWorkoutRepositoryTest {

    private lateinit var db: HomeStrengthDatabase
    private lateinit var repository: HomeStrengthRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
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
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun startWorkout_createsOneLogPerExerciseSlot() = runBlocking {
        val sessionId = repository.startWorkout(TrainingMode.NORMAL)
        val session = repository.getSession(sessionId)
        assertNotNull(session)
        val names = session!!.logs.map { it.exercise.name }
        assertEquals(names.distinct().size, names.size)
        assertEquals(5, session.logs.size)
        session.logs.forEach { log ->
            assertEquals(2, log.sets.map { it.set.setNumber }.toSet().size)
        }
    }

    @Test
    fun completeWorkout_marksCompleted_andFlipsNextType() = runBlocking {
        assertEquals(WorkoutType.A, repository.getSettings().nextWorkoutType)
        val sessionId = repository.startWorkout(TrainingMode.NORMAL)
        val before = repository.getSession(sessionId)!!
        before.logs.forEach { log ->
            log.sets.forEach { sw ->
                if (log.exercise.targetUnit == TargetUnit.REPS) {
                    repository.updateSetValue(sw.set.id, 8, TargetUnit.REPS)
                } else {
                    repository.updateSetValue(sw.set.id, 30, TargetUnit.SECONDS)
                }
            }
        }

        val completed = repository.completeWorkout(sessionId, feeling = 4)
        assertNotNull(completed)
        assertTrue(completed!!.session.completed)
        assertEquals(4, completed.session.feeling)
        assertEquals(WorkoutType.B, repository.getSettings().nextWorkoutType)
        assertEquals(null, repository.observeIncomplete().let {
            // one-shot read via DAO
            db.workoutSessionDao().getIncomplete()
        })
    }

    @Test
    fun seedDedup_keepsSinglePushupSlot() = runBlocking {
        // Simulate legacy duplicate seed rows.
        db.exerciseDao().insertAll(SeedData.defaultExercises())
        db.seedIfNeeded()
        val exercises = repository.getExercises(WorkoutType.A)
        assertEquals(5, exercises.size)
        assertEquals(1, exercises.count { it.name == "俯卧撑" })
    }

    @Test
    fun seed_clearsSkippedFlagsOnIncompleteSession() = runBlocking {
        val sessionId = repository.startWorkout(TrainingMode.NORMAL)
        val session = repository.getSession(sessionId)!!
        session.logs.forEach { repository.skipExercise(it.log.id) }
        assertTrue(repository.getSession(sessionId)!!.logs.all { it.log.skipped })

        db.seedIfNeeded()

        val fixed = repository.getSession(sessionId)!!
        assertTrue(fixed.logs.none { it.log.skipped })
    }
}
