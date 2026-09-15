package com.homestrength.domain.workout

import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SetWithBands
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviousPerformanceFormatterTest {

    @Test
    fun formatsBilateralWithResistance() {
        val log = ExerciseLogWithSets(
            log = com.homestrength.data.local.entity.ExerciseLogEntity(
                id = 1, sessionId = 1, exerciseId = 1, sortOrder = 0, prescribedSets = 2
            ),
            exercise = com.homestrength.data.local.entity.ExerciseEntity(
                id = 1,
                name = "划船",
                workoutType = com.homestrength.data.local.entity.WorkoutType.A,
                targetMin = 8,
                targetMax = 15,
                targetUnit = TargetUnit.REPS,
                sortOrder = 1,
                movementRole = com.homestrength.data.local.entity.MovementRole.PULL
            ),
            sets = listOf(
                SetWithBands(
                    set = ExerciseSetEntity(id = 1, exerciseLogId = 1, setNumber = 1, reps = 12, totalResistance = 20),
                    bands = emptyList(),
                    bandUsages = emptyList()
                ),
                SetWithBands(
                    set = ExerciseSetEntity(id = 2, exerciseLogId = 1, setNumber = 2, reps = 11, totalResistance = 20),
                    bands = emptyList(),
                    bandUsages = emptyList()
                )
            )
        )
        val previous = PreviousPerformanceFormatter.fromLog(log)
        assertEquals(20, previous.totalResistance)
        assertEquals("12 / 11", previous.valuesLabel)
        assertTrue(previous.summaryLabel.contains("20 LB"))
    }
}
