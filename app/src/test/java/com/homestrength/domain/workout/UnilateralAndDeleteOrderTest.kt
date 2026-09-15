package com.homestrength.domain.workout

import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.ExerciseLogWithSets
import com.homestrength.data.local.relation.SetWithBands
import com.homestrength.domain.progression.ProgressionEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class UnilateralAndDeleteOrderTest {

    @Test
    fun unilateralSetsExtractLeftAndRightValues() {
        val log = ExerciseLogWithSets(
            log = ExerciseLogEntity(
                id = 1,
                sessionId = 10,
                exerciseId = 5,
                sortOrder = 0,
                prescribedSets = 1
            ),
            exercise = ExerciseEntity(
                id = 5,
                name = "Dead Bug",
                workoutType = WorkoutType.A,
                targetMin = 6,
                targetMax = 10,
                targetUnit = TargetUnit.REPS,
                isUnilateral = true,
                sortOrder = 5,
                movementRole = MovementRole.CORE
            ),
            sets = listOf(
                SetWithBands(
                    set = ExerciseSetEntity(
                        id = 1,
                        exerciseLogId = 1,
                        setNumber = 1,
                        side = SetSide.LEFT,
                        reps = 8
                    ),
                    bands = emptyList(),
                    bandUsages = emptyList()
                ),
                SetWithBands(
                    set = ExerciseSetEntity(
                        id = 2,
                        exerciseLogId = 1,
                        setNumber = 1,
                        side = SetSide.RIGHT,
                        reps = 7
                    ),
                    bands = emptyList(),
                    bandUsages = emptyList()
                )
            )
        )
        val extracted = ProgressionEngine.extractPrevious(log)
        assertEquals(listOf(8, 7), extracted.values)
        val formatted = PreviousPerformanceFormatter.fromLog(log)
        assertEquals("8 / 7", formatted.valuesLabel)
    }

    @Test
    fun deletingLastCompletionRecomputesNextType() {
        // Completed A,B,A → next should be B. Remove last A → completed A,B → next A.
        val afterThree = WorkoutPlanner.recomputeNextType(
            listOf(WorkoutType.A, WorkoutType.B, WorkoutType.A)
        )
        assertEquals(WorkoutType.B, afterThree)

        val afterDeleteLast = WorkoutPlanner.recomputeNextType(
            listOf(WorkoutType.A, WorkoutType.B)
        )
        assertEquals(WorkoutType.A, afterDeleteLast)
    }
}
