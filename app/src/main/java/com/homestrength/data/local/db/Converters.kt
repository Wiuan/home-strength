package com.homestrength.data.local.db

import androidx.room.TypeConverter
import com.homestrength.data.local.entity.MovementRole
import com.homestrength.data.local.entity.PeriodGoalStatus
import com.homestrength.data.local.entity.PowerItemStatus
import com.homestrength.data.local.entity.PracticeTrack
import com.homestrength.data.local.entity.SetSide
import com.homestrength.data.local.entity.TargetUnit
import com.homestrength.data.local.entity.TrainingMode
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.domain.trainee.PlanPeriodKind

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

    @TypeConverter
    fun fromPracticeTrack(value: PracticeTrack?): String? = value?.name

    @TypeConverter
    fun toPracticeTrack(value: String?): PracticeTrack? = value?.let { PracticeTrack.valueOf(it) }

    @TypeConverter
    fun fromPowerItemStatus(value: PowerItemStatus): String = value.name

    @TypeConverter
    fun toPowerItemStatus(value: String): PowerItemStatus = PowerItemStatus.valueOf(value)

    @TypeConverter
    fun fromPeriodGoalStatus(value: PeriodGoalStatus): String = value.name

    @TypeConverter
    fun toPeriodGoalStatus(value: String): PeriodGoalStatus = PeriodGoalStatus.valueOf(value)

    @TypeConverter
    fun fromPlanPeriodKind(value: PlanPeriodKind): String = value.name

    @TypeConverter
    fun toPlanPeriodKind(value: String): PlanPeriodKind = PlanPeriodKind.valueOf(value)
}
