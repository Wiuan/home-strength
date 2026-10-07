package com.homestrength.ui.workout

import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SetWithBands
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatSetSummaryTest {

    @Test
    fun bilateral_twoSets() {
        val summary = formatSetSummary(
            log(
                unilateral = false,
                sets = listOf(
                    set(1, null, 8),
                    set(2, null, 7)
                )
            )
        )
        assertEquals("8 / 7", summary)
    }

    @Test
    fun unilateral_twoSetsAsPairs() {
        val summary = formatSetSummary(
            log(
                unilateral = true,
                sets = listOf(
                    set(1, SetSide.LEFT, 8),
                    set(1, SetSide.RIGHT, 7),
                    set(2, SetSide.LEFT, 6),
                    set(2, SetSide.RIGHT, 6)
                )
            )
        )
        assertEquals("8 / 7 · 6 / 6", summary)
    }

    private fun log(unilateral: Boolean, sets: List<ExerciseSetEntity>): ExerciseLogWithSets {
        val exercise = ExerciseEntity(
            id = 1,
            name = "测试",
            workoutType = WorkoutType.A,
            targetMin = 6,
            targetMax = 12,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = unilateral,
            sortOrder = 1,
            movementRole = MovementRole.PUSH
        )
        return ExerciseLogWithSets(
            log = ExerciseLogEntity(
                id = 10,
                sessionId = 1,
                exerciseId = 1,
                skipped = false,
                sortOrder = 0,
                prescribedSets = 2
            ),
            exercise = exercise,
            sets = sets.map { SetWithBands(set = it, bands = emptyList(), bandUsages = emptyList()) }
        )
    }

    private fun set(number: Int, side: SetSide?, reps: Int) = ExerciseSetEntity(
        id = number.toLong() * 10 + (side?.ordinal ?: 0),
        exerciseLogId = 10,
        setNumber = number,
        side = side,
        reps = reps
    )
}
