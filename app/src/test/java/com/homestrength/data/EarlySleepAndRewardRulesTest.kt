package com.homestrength.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.TraineeRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EarlySleepAndRewardRulesTest {

    private lateinit var db: HomeStrengthDatabase
    private lateinit var trainee: TraineeRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HomeStrengthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trainee = TraineeRepository(db.traineeDao())
        db.traineeDao().upsertProfile(
            TraineeProfileEntity(energy = 40, energyDate = trainee.todayKey())
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun earlySleep_restoresEnergyOncePerDay() = runBlocking {
        assertNull(trainee.completeEarlySleep())
        val after = trainee.getProfile()
        assertEquals(60, after.energy)
        assertEquals(trainee.todayKey(), after.sleepDate)
        assertEquals(10, after.fans)
        assertEquals(2, after.coins)

        assertEquals("今天已经早睡打卡过了", trainee.completeEarlySleep())
        assertEquals(60, trainee.getProfile().energy)
    }

    @Test
    fun earlySleep_deleteClearsLock() = runBlocking {
        assertNull(trainee.completeEarlySleep())
        val id = db.traineeDao().getPowerList(trainee.todayKey())
            .first { it.title == "早睡" }.id
        trainee.deletePowerItem(id)
        val after = trainee.getProfile()
        assertEquals("", after.sleepDate)
        assertEquals(40, after.energy)
        assertEquals(0, after.fans)
        assertEquals(0, after.coins)
        assertNull(trainee.completeEarlySleep())
    }

    @Test
    fun editableRules_applyToChecklist() = runBlocking {
        trainee.updateRewardRules(
            checklistFans = 7,
            checklistCoins = 4,
            lightFans = 40,
            lightCoins = 5,
            strengthFans = 100,
            strengthCoins = 12,
            sleepEnergyRestore = 20,
            sleepFans = 10,
            sleepCoins = 2,
            meditationEnergyRestore = 5,
            meditationFans = 5,
            meditationCoins = 1
        )
        trainee.addPowerItem("自定义", null)
        val id = db.traineeDao().getPowerList(trainee.todayKey()).first().id
        trainee.togglePowerItemDone(id)
        val profile = trainee.getProfile()
        assertEquals(7, profile.fans)
        assertEquals(4, profile.coins)
        assertTrue(profile.checklistCoins == 4)
    }
}
