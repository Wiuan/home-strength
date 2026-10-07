package com.homestrength.domain.trainee

import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.local.entity.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class MonthReviewTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val month = YearMonth.of(2026, 10)

    @Test
    fun aggregatesChecklistAndStrengthWithoutDoubleCount() {
        val day = LocalDate.of(2026, 10, 7)
        val millis = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val review = MonthReviewBuilder.build(
            month = month,
            powerDone = listOf(
                PowerListItemEntity(
                    dayKey = day.toString(),
                    title = "晾衣服",
                    track = PracticeTrack.LIFE,
                    status = PowerItemStatus.DONE
                ),
                PowerListItemEntity(
                    dayKey = day.toString(),
                    title = "力量训练",
                    track = PracticeTrack.STRENGTH,
                    status = PowerItemStatus.DONE
                )
            ),
            lightDone = listOf(
                LightPracticeEntity(
                    track = PracticeTrack.ALGORITHM,
                    startedAt = millis + 3_600_000,
                    durationSeconds = 1200,
                    completed = true
                )
            ),
            strengthDone = listOf(
                WorkoutSessionEntity(
                    workoutType = WorkoutType.A,
                    trainingMode = TrainingMode.NORMAL,
                    dateTime = millis,
                    completed = true,
                    updatedAt = millis + 35 * 60 * 1000L
                )
            ),
            zone = zone
        )
        val activity = review.activityOn(day)
        assertEquals(3, activity.doneCount)
        assertTrue(activity.tracks.contains(PracticeTrack.LIFE))
        assertTrue(activity.tracks.contains(PracticeTrack.STRENGTH))
        assertTrue(activity.tracks.contains(PracticeTrack.ALGORITHM))
        assertEquals(1, review.activeDays)
        assertEquals(3, review.totalDone)
        assertTrue(review.trackShares.any { it.track == PracticeTrack.ALGORITHM && it.seconds == 1200 })
        assertTrue(review.trackShares.any { it.track == PracticeTrack.STRENGTH && it.seconds == 35 * 60 })
        assertTrue(review.trackShares.any { it.track == PracticeTrack.LIFE && it.seconds == 5 * 60 })
    }

    @Test
    fun strengthDurationFallsBackWhenSpanMissing() {
        val millis = LocalDate.of(2026, 10, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val review = MonthReviewBuilder.build(
            month = month,
            powerDone = emptyList(),
            lightDone = emptyList(),
            strengthDone = listOf(
                WorkoutSessionEntity(
                    workoutType = WorkoutType.B,
                    trainingMode = TrainingMode.NORMAL,
                    dateTime = millis,
                    completed = true,
                    updatedAt = millis
                )
            ),
            zone = zone
        )
        assertEquals(40 * 60, review.trackShares.single { it.track == PracticeTrack.STRENGTH }.seconds)
    }
}
