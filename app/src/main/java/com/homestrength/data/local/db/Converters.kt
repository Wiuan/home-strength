package com.homestrength.data.local.db

import androidx.room.TypeConverter
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType

class Converters {
    @TypeConverter
    fun fromWorkoutType(value: WorkoutType): String = value.name

    @TypeConverter
    fun toWorkoutType(value: String): WorkoutType = WorkoutType.valueOf(value)

    @TypeConverter
    fun fromTargetUnit(value: TargetUnit): String = value.name

    @TypeConverter
    fun toTargetUnit(value: String): TargetUnit = TargetUnit.valueOf(value)

    @TypeConverter
    fun fromMovementRole(value: MovementRole): String = value.name

    @TypeConverter
    fun toMovementRole(value: String): MovementRole = MovementRole.valueOf(value)

    @TypeConverter
    fun fromTrainingMode(value: TrainingMode): String = value.name

    @TypeConverter
    fun toTrainingMode(value: String): TrainingMode = TrainingMode.valueOf(value)

    @TypeConverter
    fun fromSetSide(value: SetSide?): String? = value?.name

    @TypeConverter
    fun toSetSide(value: String?): SetSide? = value?.let { SetSide.valueOf(it) }
}
