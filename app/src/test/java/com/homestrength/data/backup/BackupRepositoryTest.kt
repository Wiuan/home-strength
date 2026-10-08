package com.homestrength.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.repository.TraineeRepository
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
class BackupRepositoryTest {

    private lateinit var db: HomeStrengthDatabase
    private lateinit var backup: BackupRepository
    private lateinit var trainee: TraineeRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, HomeStrengthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backup = BackupRepository(db)
        trainee = TraineeRepository(db.traineeDao())
        db.traineeDao().upsertProfile(TraineeProfileEntity(nickname = "Old", fans = 10, coins = 1))
        db.appSettingsDao().upsert(AppSettingsEntity(nextWorkoutType = WorkoutType.B, weeklyGoal = 2))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun replaceAll_restoresProfileChecklistAndSettings() = runBlocking {
        trainee.addPowerItem("阅读 10 分钟", PracticeTrack.CULTIVATION, 600)
        val exported = backup.buildPayload()

        // Mutate local state
        db.traineeDao().upsertProfile(TraineeProfileEntity(nickname = "Wiped", fans = 0, coins = 0))
        trainee.addPowerItem("临时", PracticeTrack.LIFE)

        backup.replaceAll(exported)

        val profile = db.traineeDao().getProfile()!!
        assertEquals("Old", profile.nickname)
        assertEquals(10, profile.fans)
        assertEquals(2, db.appSettingsDao().get()!!.weeklyGoal)
        assertEquals(WorkoutType.B, db.appSettingsDao().get()!!.nextWorkoutType)

        val list = db.traineeDao().getAllPowerItems()
        assertEquals(1, list.size)
        assertEquals("阅读 10 分钟", list.first().title)
        assertEquals(600, list.first().targetDurationSeconds)
        assertEquals(PracticeTrack.CULTIVATION, list.first().track)
    }

    @Test
    fun encodeDecode_viaRepositoryPayload() = runBlocking {
        trainee.addPowerItem("晾衣服", PracticeTrack.LIFE)
        val json = BackupCodec.encode(backup.buildPayload())
        val decoded = BackupCodec.decode(json)
        assertEquals(1, decoded.powerList.size)
        assertEquals("晾衣服", decoded.powerList.first().title)
    }
}
