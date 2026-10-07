package com.homestrength.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.TraineeRewards
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
