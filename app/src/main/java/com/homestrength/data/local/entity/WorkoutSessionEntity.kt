package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["dateTime"]), Index(value = ["completed"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTime: Long,
    val workoutType: WorkoutType,
    val trainingMode: TrainingMode,
    val feeling: Int? = null,
    val completed: Boolean = false,
    val updatedAt: Long = dateTime
)
