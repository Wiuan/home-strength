package com.homestrength.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.SetBandCrossRef
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.local.relation.SessionWithLogs
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Insert
    suspend fun insertLog(log: ExerciseLogEntity): Long

    @Insert
    suspend fun insertLogs(logs: List<ExerciseLogEntity>): List<Long>

    @Update
    suspend fun updateLog(log: ExerciseLogEntity)

    @Insert
    suspend fun insertSet(set: ExerciseSetEntity): Long

    @Update
    suspend fun updateSet(set: ExerciseSetEntity)

    @Query("SELECT * FROM exercise_sets WHERE id = :setId")
    suspend fun getSetById(setId: Long): ExerciseSetEntity?

    @Query("SELECT sessionId FROM exercise_logs WHERE id = (SELECT exerciseLogId FROM exercise_sets WHERE id = :setId)")
    suspend fun getSessionIdForSet(setId: Long): Long?

    @Query("SELECT * FROM exercise_logs WHERE id = :logId")
    suspend fun getLogById(logId: Long): ExerciseLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetBands(refs: List<SetBandCrossRef>)

    @Query("DELETE FROM set_band_cross_ref WHERE setId = :setId")
    suspend fun clearSetBands(setId: Long)

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionWithLogs(sessionId: Long): SessionWithLogs?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun observeSessionWithLogs(sessionId: Long): Flow<SessionWithLogs?>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY dateTime DESC")
    fun observeAllSessions(): Flow<List<SessionWithLogs>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completed = 1 ORDER BY dateTime DESC LIMIT 1")
    fun observeLastCompleted(): Flow<SessionWithLogs?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completed = 1 ORDER BY dateTime DESC LIMIT :limit")
    fun observeRecentCompleted(limit: Int): Flow<List<SessionWithLogs>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE completed = 0 ORDER BY updatedAt DESC LIMIT 1")
    fun observeIncomplete(): Flow<SessionWithLogs?>

    @Query("SELECT * FROM workout_sessions WHERE completed = 0 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getIncomplete(): WorkoutSessionEntity?

    @Query(
        """
        SELECT COUNT(*) FROM workout_sessions
        WHERE completed = 1
          AND dateTime >= :weekStartMillis
          AND dateTime < :weekEndMillis
        """
    )
    fun observeCompletedCountInRange(weekStartMillis: Long, weekEndMillis: Long): Flow<Int>

    @Query(
        """
        SELECT * FROM workout_sessions
        WHERE completed = 1
          AND dateTime >= :startMillis
          AND dateTime < :endMillis
        ORDER BY dateTime ASC
        """
    )
    suspend fun getCompletedInRange(startMillis: Long, endMillis: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE completed = 1 ORDER BY dateTime ASC")
    suspend fun getAllCompletedOrdered(): List<WorkoutSessionEntity>

    @Query("SELECT id FROM workout_sessions")
    suspend fun getAllSessionIds(): List<Long>

    @Query("SELECT * FROM exercise_logs WHERE sessionId = :sessionId ORDER BY sortOrder ASC, id ASC")
    suspend fun getLogsForSession(sessionId: Long): List<ExerciseLogEntity>

    @Query("SELECT * FROM exercise_sets WHERE exerciseLogId = :logId")
    suspend fun getSetsForLog(logId: Long): List<ExerciseSetEntity>

    @Query("DELETE FROM exercise_logs WHERE id = :logId")
    suspend fun deleteLog(logId: Long)
}
