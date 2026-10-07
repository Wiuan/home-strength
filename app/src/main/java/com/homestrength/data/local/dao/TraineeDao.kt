package com.homestrength.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.RewardEntity
import com.homestrength.data.local.entity.RewardRedemptionEntity
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.domain.trainee.PlanPeriodKind
import kotlinx.coroutines.flow.Flow

@Dao
interface TraineeDao {
    @Query("SELECT * FROM trainee_profile WHERE id = 1")
    fun observeProfile(): Flow<TraineeProfileEntity?>

    @Query("SELECT * FROM trainee_profile WHERE id = 1")
    suspend fun getProfile(): TraineeProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: TraineeProfileEntity)

    @Update
    suspend fun updateProfile(profile: TraineeProfileEntity)

    @Query("SELECT * FROM power_list_items WHERE dayKey = :dayKey ORDER BY sortOrder ASC, id ASC")
    fun observePowerList(dayKey: String): Flow<List<PowerListItemEntity>>

    @Query("SELECT * FROM power_list_items WHERE dayKey = :dayKey ORDER BY sortOrder ASC, id ASC")
    suspend fun getPowerList(dayKey: String): List<PowerListItemEntity>

    @Insert
    suspend fun insertPowerItem(item: PowerListItemEntity): Long

    @Update
    suspend fun updatePowerItem(item: PowerListItemEntity)

    @Query("DELETE FROM power_list_items WHERE id = :id")
    suspend fun deletePowerItem(id: Long)

    @Insert
    suspend fun insertLightPractice(practice: LightPracticeEntity): Long

    @Update
    suspend fun updateLightPractice(practice: LightPracticeEntity)

    @Query("SELECT * FROM light_practices WHERE id = :id")
    suspend fun getLightPractice(id: Long): LightPracticeEntity?

    @Query("SELECT * FROM light_practices WHERE completed = 1 ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecentLightPractices(limit: Int = 20): Flow<List<LightPracticeEntity>>

    @Query(
        """
        SELECT * FROM light_practices
        WHERE completed = 1 AND startedAt >= :startMillis AND startedAt < :endMillis
        ORDER BY startedAt ASC
        """
    )
    suspend fun getCompletedLightInRange(startMillis: Long, endMillis: Long): List<LightPracticeEntity>

    @Query(
        """
        SELECT * FROM power_list_items
        WHERE dayKey >= :startDay AND dayKey < :endDayExclusive AND status = 'DONE'
        ORDER BY dayKey ASC, sortOrder ASC
        """
    )
    suspend fun getDonePowerItemsInRange(startDay: String, endDayExclusive: String): List<PowerListItemEntity>

    @Query(
        """
        SELECT * FROM period_goals
        WHERE periodKind = :kind AND periodKey = :periodKey
        ORDER BY sortOrder ASC, id ASC
        """
    )
    fun observePeriodGoals(kind: PlanPeriodKind, periodKey: String): Flow<List<PeriodGoalEntity>>

    @Query(
        """
        SELECT * FROM period_goals
        WHERE periodKind = :kind AND periodKey = :periodKey
        ORDER BY sortOrder ASC, id ASC
        """
    )
    suspend fun getPeriodGoals(kind: PlanPeriodKind, periodKey: String): List<PeriodGoalEntity>

    @Query("SELECT * FROM period_goals WHERE id = :id")
    suspend fun getPeriodGoal(id: Long): PeriodGoalEntity?

    @Insert
    suspend fun insertPeriodGoal(goal: PeriodGoalEntity): Long

    @Update
    suspend fun updatePeriodGoal(goal: PeriodGoalEntity)

    @Query("DELETE FROM period_goals WHERE id = :id")
    suspend fun deletePeriodGoal(id: Long)

    @Query("SELECT * FROM rewards ORDER BY sortOrder ASC, id ASC")
    fun observeRewards(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards ORDER BY sortOrder ASC, id ASC")
    suspend fun getRewards(): List<RewardEntity>

    @Query("SELECT * FROM rewards WHERE id = :id")
    suspend fun getReward(id: Long): RewardEntity?

    @Insert
    suspend fun insertReward(reward: RewardEntity): Long

    @Update
    suspend fun updateReward(reward: RewardEntity)

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun deleteReward(id: Long)

    @Query("SELECT COUNT(*) FROM rewards")
    suspend fun rewardCount(): Int

    @Insert
    suspend fun insertRedemption(redemption: RewardRedemptionEntity): Long

    @Query("SELECT * FROM reward_redemptions ORDER BY redeemedAt DESC LIMIT :limit")
    fun observeRedemptions(limit: Int = 20): Flow<List<RewardRedemptionEntity>>
}
