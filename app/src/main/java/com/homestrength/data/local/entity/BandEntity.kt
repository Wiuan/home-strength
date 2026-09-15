package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "bands",
    indices = [Index(value = ["resistance"], unique = true)]
)
data class BandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val resistance: Int,
    val quantity: Int = 1,
    val enabled: Boolean = true
)
