package com.homestrength.data.seed

import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.WorkoutType

object SeedData {
    fun defaultBands(): List<BandEntity> = listOf(
        BandEntity(resistance = 10, quantity = 1),
        BandEntity(resistance = 15, quantity = 1),
        BandEntity(resistance = 20, quantity = 1),
        BandEntity(resistance = 25, quantity = 1),
        BandEntity(resistance = 30, quantity = 1)
    )

    fun defaultExercises(): List<ExerciseEntity> = listOf(
        // Workout A
        ExerciseEntity(
            name = "俯卧撑",
            workoutType = WorkoutType.A,
            targetMin = 6,
            targetMax = 12,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "标准俯卧撑，保持核心稳定。",
            sortOrder = 1,
            movementRole = MovementRole.PUSH
        ),
        ExerciseEntity(
            name = "弹力带划船",
            workoutType = WorkoutType.A,
            targetMin = 8,
            targetMax = 15,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "坐姿或站姿划船，肩胛后收。",
            sortOrder = 2,
            movementRole = MovementRole.PULL
        ),
        ExerciseEntity(
            name = "深蹲",
            workoutType = WorkoutType.A,
            targetMin = 8,
            targetMax = 15,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "双脚约肩宽，膝盖跟随脚尖方向。",
            sortOrder = 3,
            movementRole = MovementRole.SQUAT
        ),
        ExerciseEntity(
            name = "弹力带 RDL",
            workoutType = WorkoutType.A,
            targetMin = 8,
            targetMax = 15,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "髋铰链，感受腘绳肌与臀。",
            sortOrder = 4,
            movementRole = MovementRole.HINGE
        ),
        ExerciseEntity(
            name = "Dead Bug",
            workoutType = WorkoutType.A,
            targetMin = 6,
            targetMax = 10,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = true,
            description = "每侧次数分别记录。",
            sortOrder = 5,
            movementRole = MovementRole.CORE
        ),
        // Workout B
        ExerciseEntity(
            name = "弹力带肩推",
            workoutType = WorkoutType.B,
            targetMin = 8,
            targetMax = 15,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "站姿或坐姿肩推，避免过度后仰。",
            sortOrder = 1,
            movementRole = MovementRole.PUSH
        ),
        ExerciseEntity(
            name = "弹力带划船",
            workoutType = WorkoutType.B,
            targetMin = 8,
            targetMax = 15,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "坐姿或站姿划船，肩胛后收。",
            sortOrder = 2,
            movementRole = MovementRole.PULL
        ),
        ExerciseEntity(
            name = "保加利亚分腿蹲",
            workoutType = WorkoutType.B,
            targetMin = 6,
            targetMax = 10,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = true,
            description = "后脚垫高，前腿承重；左右侧分别记录。",
            sortOrder = 3,
            movementRole = MovementRole.SQUAT
        ),
        ExerciseEntity(
            name = "臀桥",
            workoutType = WorkoutType.B,
            targetMin = 10,
            targetMax = 15,
            targetUnit = TargetUnit.REPS,
            defaultSets = 2,
            isUnilateral = false,
            description = "顶峰稍停，臀肌发力。",
            sortOrder = 4,
            movementRole = MovementRole.HINGE
        ),
        ExerciseEntity(
            name = "平板支撑",
            workoutType = WorkoutType.B,
            targetMin = 20,
            targetMax = 40,
            targetUnit = TargetUnit.SECONDS,
            defaultSets = 2,
            isUnilateral = false,
            description = "身体成一条直线，计时秒数。",
            sortOrder = 5,
            movementRole = MovementRole.CORE
        )
    )
}
