package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["workoutType", "sortOrder"])]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val workoutType: WorkoutType,
    val targetMin: Int,
    val targetMax: Int,
    val targetUnit: TargetUnit,
    val defaultSets: Int = 2,
    val isUnilateral: Boolean = false,
    val description: String = "",
    val sortOrder: Int,
    val movementRole: MovementRole
)
