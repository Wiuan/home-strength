package com.homestrength.domain.workout

import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutPlannerTest {

    @Test
    fun alternatesAAndB() {
        assertEquals(WorkoutType.B, WorkoutPlanner.nextType(WorkoutType.A))
        assertEquals(WorkoutType.A, WorkoutPlanner.nextType(WorkoutType.B))
    }

    @Test
    fun skippingDayDoesNotBreakOrder_recomputeFromCompletionsOnly() {
        // Completed A then B; skipped a calendar day in between does not matter.
        val next = WorkoutPlanner.recomputeNextType(listOf(WorkoutType.A, WorkoutType.B))
        assertEquals(WorkoutType.A, next)
    }

    @Test
    fun exhaustedModeKeepsOnlyPushPullSquat() {
        val exercises = listOf(
            exercise("肩推", 1, MovementRole.PUSH),
            exercise("划船", 2, MovementRole.PULL),
            exercise("分腿蹲", 3, MovementRole.SQUAT),
            exercise("臀桥", 4, MovementRole.HINGE),
            exercise("平板", 5, MovementRole.CORE)
        )
        val selected = WorkoutPlanner.selectExercises(exercises, TrainingMode.EXHAUSTED)
        assertEquals(listOf("肩推", "划船", "分腿蹲"), selected.map { it.name })
        assertEquals(1, WorkoutPlanner.prescribedSets(TrainingMode.EXHAUSTED, exerciseDefaultSets = 2))
    }

    private fun exercise(name: String, order: Int, role: MovementRole) = ExerciseEntity(
        id = order.toLong(),
        name = name,
        workoutType = WorkoutType.B,
        targetMin = 8,
        targetMax = 15,
        targetUnit = TargetUnit.REPS,
        sortOrder = order,
        movementRole = role
    )
}
