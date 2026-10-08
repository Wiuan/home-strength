package com.homestrength.data.backup

import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PeriodGoalStatus
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.domain.trainee.PlanPeriodKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupCodecTest {

    @Test
    fun roundTrip_preservesChecklistAndProfile() {
        val payload = BackupCodec.Payload(
            exportedAt = 1_700_000_000_000L,
            dbVersion = 12,
            profile = TraineeProfileEntity(nickname = "W", fans = 120, coins = 33),
            settings = AppSettingsEntity(weeklyGoal = 4),
            exercises = listOf(
                ExerciseEntity(
                    id = 1,
                    name = "俯卧撑",
                    workoutType = WorkoutType.A,
                    targetMin = 8,
                    targetMax = 12,
                    targetUnit = TargetUnit.REPS,
                    sortOrder = 0,
                    movementRole = MovementRole.PUSH
                )
            ),
            bands = listOf(BandEntity(id = 2, resistance = 15, quantity = 2)),
            sessions = emptyList(),
            logs = emptyList(),
            sets = emptyList(),
            setBands = emptyList(),
            powerList = listOf(
                PowerListItemEntity(
                    id = 9,
                    dayKey = "2026-10-08",
                    title = "阅读 10 分钟",
                    track = PracticeTrack.CULTIVATION,
                    status = PowerItemStatus.PENDING,
                    targetDurationSeconds = 600,
                    practicedMinutes = 0
                )
            ),
            lightPractices = listOf(
                LightPracticeEntity(
                    id = 3,
                    track = PracticeTrack.CULTIVATION,
                    startedAt = 1_700_000_000_000L,
                    durationSeconds = 600,
                    completed = true,
                    fansEarned = 40,
                    coinsEarned = 5
                )
            ),
            periodGoals = listOf(
                PeriodGoalEntity(
                    id = 4,
                    periodKind = PlanPeriodKind.MONTH,
                    periodKey = "2026-10",
                    title = "阅读 20 页",
                    track = PracticeTrack.CULTIVATION,
                    status = PeriodGoalStatus.ACTIVE,
                    targetParts = 12,
                    doneParts = 2
                )
            ),
            rewards = emptyList(),
            redemptions = emptyList()
        )

        val decoded = BackupCodec.decode(BackupCodec.encode(payload))
        assertEquals(BackupCodec.FORMAT_VERSION, 1)
        assertEquals("W", decoded.profile.nickname)
        assertEquals(120, decoded.profile.fans)
        assertEquals(4, decoded.settings.weeklyGoal)
        assertEquals(1, decoded.exercises.size)
        assertEquals("俯卧撑", decoded.exercises.first().name)
        assertEquals(15, decoded.bands.first().resistance)
        assertEquals("阅读 10 分钟", decoded.powerList.first().title)
        assertEquals(600, decoded.powerList.first().targetDurationSeconds)
        assertEquals(PracticeTrack.CULTIVATION, decoded.lightPractices.first().track)
        assertEquals(12, decoded.periodGoals.first().targetParts)
        assertEquals(2, decoded.periodGoals.first().doneParts)
    }

    @Test
    fun decode_rejectsWrongFormat() {
        val err = runCatching { BackupCodec.decode("""{"format":"other","version":1}""") }
        assertTrue(err.isFailure)
        assertTrue(err.exceptionOrNull()!!.message!!.contains("不是练习生"))
    }
}
