package com.homestrength.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.WorkoutType
import com.homestrength.data.local.relation.ExerciseLogWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY workoutType, sortOrder")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutType = :type ORDER BY sortOrder")
    fun observeByWorkoutType(type: WorkoutType): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE workoutType = :type ORDER BY sortOrder")
    suspend fun getByWorkoutType(type: WorkoutType): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Transaction
    @Query(
        """
        SELECT el.* FROM exercise_logs el
        INNER JOIN workout_sessions ws ON ws.id = el.sessionId
        WHERE el.exerciseId = :exerciseId
          AND el.skipped = 0
          AND ws.completed = 1
        ORDER BY ws.dateTime DESC
        LIMIT :limit
        """
    )
    suspend fun getRecentCompletedLogs(exerciseId: Long, limit: Int = 20): List<ExerciseLogWithSets>
}
