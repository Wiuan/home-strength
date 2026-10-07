package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rewards",
    indices = [Index(value = ["sortOrder"])]
)
data class RewardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val cost: Int,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "reward_redemptions",
    indices = [Index(value = ["redeemedAt"])]
)
data class RewardRedemptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardId: Long,
    val title: String,
    val cost: Int,
    val redeemedAt: Long
)
