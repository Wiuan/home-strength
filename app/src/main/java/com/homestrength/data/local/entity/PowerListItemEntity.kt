package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "power_list_items",
    indices = [Index(value = ["dayKey", "sortOrder"])]
)
data class PowerListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local date yyyy-MM-dd */
    val dayKey: String,
    val title: String,
    val track: PracticeTrack? = null,
    val status: PowerItemStatus = PowerItemStatus.PENDING,
    val sortOrder: Int = 0,
    val note: String = "",
    /** Fans granted for this item's current DONE state; clawed back when unchecked. */
    val rewardFans: Int = 0,
    /** Coins granted for this item's current DONE state; clawed back when unchecked. */
    val rewardCoins: Int = 0,
    /**
     * Planned countdown length in seconds. 0 = no timer (life tick-only, or unlimited count-up).
     */
    val targetDurationSeconds: Int = 0,
    /** Accumulated practiced minutes for this checklist row (seconds dropped). */
    val practicedMinutes: Int = 0,
    /** Active / last linked light practice id; 0 = none. */
    val linkedPracticeId: Long = 0,
    /** Done via light-track completion — no checklist reward was granted. */
    val completedViaPractice: Boolean = false
)
