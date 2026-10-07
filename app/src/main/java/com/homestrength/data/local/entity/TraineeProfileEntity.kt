package com.homestrength.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trainee_profile")
data class TraineeProfileEntity(
    @PrimaryKey val id: Int = 1,
    val nickname: String = "W",
    val fans: Int = 0,
    val coins: Int = 0,
    val energy: Int = 100,
    /** yyyy-MM-dd of last energy refresh */
    val energyDate: String = "",
    val showCultivation: Boolean = true,
    /** Editable reward rules — defaults match [com.homestrength.domain.trainee.TraineeRewards]. */
    val checklistFans: Int = 20,
    val checklistCoins: Int = 3,
    val lightFans: Int = 40,
    val lightCoins: Int = 5,
    val strengthFans: Int = 100,
    val strengthCoins: Int = 12,
    val sleepEnergyRestore: Int = 20,
    val sleepFans: Int = 10,
    val sleepCoins: Int = 2,
    /** yyyy-MM-dd of last early-sleep check-in (once per day). */
    val sleepDate: String = "",
    val meditationEnergyRestore: Int = 5,
    val meditationFans: Int = 5,
    val meditationCoins: Int = 1,
    /**
     * Last grade that already played the upgrade fireworks.
     * Empty means "sync quietly on first load" (no celebration).
     */
    val celebratedGrade: String = "",
    /**
     * Star coins per 1 yuan when spending from clothing / travel funds.
     * Default 10 → 100 币 ≈ 10 元.
     */
    val coinsPerYuan: Int = 10
)
