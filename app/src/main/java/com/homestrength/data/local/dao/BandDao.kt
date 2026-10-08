package com.homestrength.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.homestrength.data.local.entity.BandEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BandDao {
    @Query("SELECT * FROM bands ORDER BY resistance ASC")
    fun observeAll(): Flow<List<BandEntity>>

    @Query("SELECT * FROM bands WHERE enabled = 1 ORDER BY resistance ASC")
    suspend fun getEnabled(): List<BandEntity>

    @Query("SELECT * FROM bands ORDER BY resistance ASC")
    suspend fun getAll(): List<BandEntity>

    @Query("SELECT * FROM bands WHERE resistance = :resistance LIMIT 1")
    suspend fun getByResistance(resistance: Int): BandEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(band: BandEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bands: List<BandEntity>): List<Long>

    @Update
    suspend fun update(band: BandEntity)

    @Delete
    suspend fun delete(band: BandEntity)

    @Query("SELECT COUNT(*) FROM bands")
    suspend fun count(): Int

    @Query("DELETE FROM bands")
    suspend fun deleteAll()
}
