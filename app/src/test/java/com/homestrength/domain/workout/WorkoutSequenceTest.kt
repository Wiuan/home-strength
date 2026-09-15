package com.homestrength.domain.workout

import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSequenceTest {
    @Test
    fun completingWorkoutsAlternatesType() {
        var next = WorkoutType.A
        next = WorkoutPlanner.nextType(next)
        assertEquals(WorkoutType.B, next)
        next = WorkoutPlanner.nextType(next)
        assertEquals(WorkoutType.A, next)
        next = WorkoutPlanner.nextType(next)
        assertEquals(WorkoutType.B, next)
    }

    @Test
    fun tiredAndExhaustedUseSameExerciseFilterDifferentSets() {
        assertEquals(2, WorkoutPlanner.prescribedSets(TrainingMode.TIRED, 2))
        assertEquals(1, WorkoutPlanner.prescribedSets(TrainingMode.EXHAUSTED, 2))
    }
}
