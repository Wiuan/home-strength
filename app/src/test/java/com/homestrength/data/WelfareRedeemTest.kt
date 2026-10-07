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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WelfareRedeemTest {

    private lateinit var db: HomeStrengthDatabase
    private lateinit var trainee: TraineeRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HomeStrengthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        trainee = TraineeRepository(db.traineeDao())
        db.traineeDao().upsertProfile(TraineeProfileEntity(coins = 35))
        trainee.seedDefaultRewardsIfEmpty()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun redeem_subtractsCoins_whenAffordable() = runBlocking {
        val reward = db.traineeDao().getRewards().first { it.cost == 20 }
        assertNull(trainee.redeemReward(reward.id))
        assertEquals(15, trainee.getProfile().coins)
    }

    @Test
    fun redeem_fails_whenNotEnoughCoins() = runBlocking {
        val reward = db.traineeDao().getRewards().first { it.cost == 40 }
        val err = trainee.redeemReward(reward.id)
        assertNotNull(err)
        assertEquals(35, trainee.getProfile().coins)
    }

    @Test
    fun fundPurchase_usesCoinsPerYuan() = runBlocking {
        db.traineeDao().upsertProfile(TraineeProfileEntity(coins = 200, coinsPerYuan = 10))
        assertNull(trainee.redeemFundPurchase("服装基金", "卫衣", yuan = 15))
        assertEquals(50, trainee.getProfile().coins) // 200 - 15*10
    }
}
