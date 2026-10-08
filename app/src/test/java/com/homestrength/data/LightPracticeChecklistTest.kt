package com.homestrength.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.domain.trainee.PlanPeriod
import com.homestrength.domain.trainee.PlanPeriodKind
import com.homestrength.domain.trainee.TraineeRewards
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LightPracticeChecklistTest {

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
            TraineeProfileEntity(
                coins = 10,
                fans = 0,
                energy = TraineeRewards.ENERGY_MAX,
                lightFans = 40,
                lightCoins = 5,
                checklistFans = 20,
                checklistCoins = 3
            )
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun completeLightPractice_linkedItem_getsLightRewardOnly() = runBlocking {
        assertTrue(
            trainee.addPowerItem(
                title = "阅读 10 分钟",
                track = PracticeTrack.CULTIVATION,
                targetDurationSeconds = 600
            )
        )
        val item = db.traineeDao().getPowerList(trainee.todayKey()).first()
        val practiceId = trainee.startLightPractice(PracticeTrack.CULTIVATION, item.id)

        val result = trainee.completeLightPractice(
            id = practiceId,
            durationSeconds = 650,
            note = "",
            noteTitle = "阅读 10 分钟",
            powerListItemId = item.id
        )

        assertEquals(40, result!!.fansEarned)
        assertEquals(5, result.coinsEarned)
        val profile = trainee.getProfile()
        assertEquals(40, profile.fans)
        assertEquals(15, profile.coins)
        assertEquals(TraineeRewards.ENERGY_MAX - 10, profile.energy)

        val updated = trainee.getPowerItem(item.id)!!
        assertEquals(PowerItemStatus.DONE, updated.status)
        assertTrue(updated.completedViaPractice)
        assertEquals(40, updated.rewardFans)
        assertEquals(5, updated.rewardCoins)
        assertEquals(10, updated.practicedMinutes)
        assertEquals(1, db.traineeDao().getPowerList(trainee.todayKey()).size)
    }

    @Test
    fun uncheckPracticeCompletedItem_clawsLightRewards() = runBlocking {
        trainee.addPowerItem("阅读", PracticeTrack.CULTIVATION, 600)
        val item = db.traineeDao().getPowerList(trainee.todayKey()).first()
        val practiceId = trainee.startLightPractice(PracticeTrack.CULTIVATION, item.id)
        trainee.completeLightPractice(practiceId, 600, "", "", item.id)

        assertEquals(40, trainee.getProfile().fans)
        assertEquals(15, trainee.getProfile().coins)
        trainee.togglePowerItemDone(item.id)
        assertEquals(0, trainee.getProfile().fans)
        assertEquals(10, trainee.getProfile().coins)
        assertEquals(PowerItemStatus.PENDING, trainee.getPowerItem(item.id)!!.status)
        assertFalse(trainee.getPowerItem(item.id)!!.completedViaPractice)
    }

    @Test
    fun deletePracticeCompletedItem_clawsLightRewards() = runBlocking {
        trainee.addPowerItem("阅读", PracticeTrack.CULTIVATION, 600)
        val item = db.traineeDao().getPowerList(trainee.todayKey()).first()
        val practiceId = trainee.startLightPractice(PracticeTrack.CULTIVATION, item.id)
        trainee.completeLightPractice(practiceId, 600, "", "", item.id)

        assertEquals(40, trainee.getProfile().fans)
        trainee.deletePowerItem(item.id)
        assertEquals(0, trainee.getProfile().fans)
        assertEquals(10, trainee.getProfile().coins)
        assertTrue(db.traineeDao().getPowerList(trainee.todayKey()).isEmpty())
    }

    @Test
    fun manualChecklistTick_stillAwardsChecklistRewards() = runBlocking {
        trainee.addPowerItem("晾衣服", PracticeTrack.LIFE)
        val id = db.traineeDao().getPowerList(trainee.todayKey()).first().id
        trainee.togglePowerItemDone(id)
        assertEquals(20, trainee.getProfile().fans)
        assertEquals(13, trainee.getProfile().coins)
    }

    @Test
    fun placePeriodGoalIntoToday_carriesTrackAndDuration() = runBlocking {
        val period = PlanPeriod.current(PlanPeriodKind.MONTH)
        assertNull(
            trainee.addPeriodGoal(
                title = "阅读 20 页",
                period = period,
                track = PracticeTrack.CULTIVATION,
                targetParts = 12
            )
        )
        val goalId = db.traineeDao().getPeriodGoals(period.kind, period.key).first().id
        assertNull(trainee.placePeriodGoalIntoToday(goalId, targetDurationSeconds = 15 * 60))

        val item = db.traineeDao().getPowerList(trainee.todayKey()).first()
        assertEquals("阅读 20 页", item.title)
        assertEquals(PracticeTrack.CULTIVATION, item.track)
        assertEquals(900, item.targetDurationSeconds)
        // Placing today does not bump period parts
        val goal = db.traineeDao().getPeriodGoal(goalId)!!
        assertEquals(0, goal.doneParts)
        assertEquals(12, goal.targetParts)
    }
}
