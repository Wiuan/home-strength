package com.homestrength.domain.workout

import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType

object WorkoutPlanner {
    fun nextType(current: WorkoutType): WorkoutType =
        if (current == WorkoutType.A) WorkoutType.B else WorkoutType.A

    /**
     * Replay completed sessions from an initial A to recompute next workout type.
     */
    fun recomputeNextType(completedTypesInOrder: List<WorkoutType>): WorkoutType {
        var next = WorkoutType.A
        completedTypesInOrder.forEach { _ ->
            next = nextType(next)
        }
        return next
    }

    fun selectExercises(
        allForType: List<ExerciseEntity>,
        mode: TrainingMode
    ): List<ExerciseEntity> {
        val ordered = allForType.sortedBy { it.sortOrder }
        return when (mode) {
            TrainingMode.NORMAL -> ordered
            TrainingMode.TIRED, TrainingMode.EXHAUSTED -> {
                listOfNotNull(
                    ordered.firstOrNull { it.movementRole == MovementRole.PUSH },
                    ordered.firstOrNull { it.movementRole == MovementRole.PULL },
                    ordered.firstOrNull { it.movementRole == MovementRole.SQUAT }
                )
            }
        }
    }

    fun prescribedSets(mode: TrainingMode, exerciseDefaultSets: Int): Int =
        when (mode) {
            TrainingMode.NORMAL, TrainingMode.TIRED -> exerciseDefaultSets
            TrainingMode.EXHAUSTED -> 1
        }
}
