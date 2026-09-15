package com.homestrength.data.local.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.SetBandCrossRef
import com.homestrength.data.local.entity.WorkoutSessionEntity

data class SetWithBands(
    @Embedded val set: ExerciseSetEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SetBandCrossRef::class,
            parentColumn = "setId",
            entityColumn = "bandId"
        )
    )
    val bands: List<BandEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "setId"
    )
    val bandUsages: List<SetBandCrossRef>
)

data class ExerciseLogWithSets(
    @Embedded val log: ExerciseLogEntity,
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity,
    @Relation(
        entity = ExerciseSetEntity::class,
        parentColumn = "id",
        entityColumn = "exerciseLogId"
    )
    val sets: List<SetWithBands>
)

data class SessionWithLogs(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = ExerciseLogEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val logs: List<ExerciseLogWithSets>
)
