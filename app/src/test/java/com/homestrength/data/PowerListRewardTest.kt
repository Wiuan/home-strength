package com.homestrength.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.MonthReviewBuilder
import com.homestrength.domain.trainee.TraineeRewards
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PowerListRewardTest {

    private lateinit var db: HomeStrengthDatabase
    private lateinit var trainee: TraineeRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HomeStrengthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trainee = TraineeRepository(db.traineeDao())
        db.traineeDao().upsertProfile(TraineeProfileEntity(coins = 10, fans = 0))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun checklistToggle_awardsAndClawsBack() = runBlocking {
        trainee.addPowerItem("早睡", null)
        val id = db.traineeDao().getPowerList(trainee.todayKey()).first().id

        trainee.togglePowerItemDone(id)
        assertEquals(10 + TraineeRewards.CHECKLIST_COINS, trainee.getProfile().coins)
        assertEquals(TraineeRewards.CHECKLIST_FANS, trainee.getProfile().fans)

        trainee.togglePowerItemDone(id)
        assertEquals(10, trainee.getProfile().coins)
        assertEquals(0, trainee.getProfile().fans)
    }

    @Test
    fun timedLightCheckbox_writesDurationIntoTrackShare() = runBlocking {
        trainee.addPowerItem("算法", PracticeTrack.ALGORITHM, targetDurationSeconds = 60 * 60)
        val id = db.traineeDao().getPowerList(trainee.todayKey()).first().id

        trainee.togglePowerItemDone(id)
        val item = db.traineeDao().getPowerList(trainee.todayKey()).first()
        assertEquals(60, item.practicedMinutes)
        assertTrue(item.completedViaPractice)
        assertEquals(TraineeRewards.LIGHT_FANS, trainee.getProfile().fans)

        val lights = db.traineeDao().getAllLightPractices()
        assertEquals(1, lights.size)
        assertEquals(3600, lights.first().durationSeconds)
        assertTrue(lights.first().completed)

        val day = LocalDate.parse(trainee.todayKey())
        val review = MonthReviewBuilder.build(
            month = YearMonth.from(day),
            powerDone = listOf(item),
            lightDone = lights,
            strengthDone = emptyList(),
            zone = ZoneId.systemDefault()
        )
        assertTrue(review.trackShares.any { it.track == PracticeTrack.ALGORITHM && it.seconds == 3600 })

        trainee.togglePowerItemDone(id)
        assertEquals(0, db.traineeDao().getAllLightPractices().size)
        assertEquals(0, trainee.getProfile().fans)
    }
}
