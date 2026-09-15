package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "set_band_cross_ref",
    primaryKeys = ["setId", "bandId"],
    foreignKeys = [
        ForeignKey(
            entity = ExerciseSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["setId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BandEntity::class,
            parentColumns = ["id"],
            childColumns = ["bandId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["bandId"])]
)
data class SetBandCrossRef(
    val setId: Long,
    val bandId: Long,
    val countUsed: Int = 1
)
