package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val nextWorkoutType: WorkoutType = WorkoutType.A,
    val weeklyGoal: Int = 3,
    val defaultSets: Int = 2,
    val defaultRestSeconds: Int = 90
)
