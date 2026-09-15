package com.homestrength.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.homestrength.data.local.dao.AppSettingsDao
import com.homestrength.data.local.dao.BandDao
import com.homestrength.data.local.dao.ExerciseDao
import com.homestrength.data.local.dao.WorkoutSessionDao
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.SetBandCrossRef
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.seed.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ExerciseEntity::class,
        BandEntity::class,
        AppSettingsEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        ExerciseSetEntity::class,
        SetBandCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HomeStrengthDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun bandDao(): BandDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        @Volatile
        private var instance: HomeStrengthDatabase? = null

        fun getInstance(context: Context): HomeStrengthDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): HomeStrengthDatabase {
            return Room.databaseBuilder(
                context,
                HomeStrengthDatabase::class.java,
                "home_strength.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).seedIfNeeded()
                        }
                    }
                })
                .build()
                .also { database ->
                    // Ensure seed also runs when DB already exists but was emptied in tests.
                    CoroutineScope(Dispatchers.IO).launch {
                        database.seedIfNeeded()
                    }
                }
        }
    }

    suspend fun seedIfNeeded() {
        val settingsDao = appSettingsDao()
        if (settingsDao.get() == null) {
            settingsDao.upsert(AppSettingsEntity())
        }
        if (exerciseDao().count() == 0) {
            exerciseDao().insertAll(SeedData.defaultExercises())
        }
        if (bandDao().count() == 0) {
            bandDao().insertAll(SeedData.defaultBands())
        }
    }
}
