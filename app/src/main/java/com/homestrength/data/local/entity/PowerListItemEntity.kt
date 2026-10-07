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
    val rewardCoins: Int = 0
)
