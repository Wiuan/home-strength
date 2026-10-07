package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "light_practices",
    indices = [Index(value = ["track", "startedAt"])]
)
data class LightPracticeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val track: PracticeTrack,
    val startedAt: Long,
    val durationSeconds: Int = 0,
    val note: String = "",
    /** Optional note title to find later on PC — not a file path. */
    val noteTitle: String = "",
    val completed: Boolean = false,
    val fansEarned: Int = 0,
    val coinsEarned: Int = 0
)
