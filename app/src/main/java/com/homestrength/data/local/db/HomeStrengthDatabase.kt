package com.homestrength.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.homestrength.data.local.dao.AppSettingsDao
import com.homestrength.data.local.dao.BandDao
import com.homestrength.data.local.dao.ExerciseDao
import com.homestrength.data.local.dao.TraineeDao
import com.homestrength.data.local.dao.WorkoutSessionDao
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.BandEntity
import com.homestrength.data.local.entity.ExerciseEntity
import com.homestrength.data.local.entity.ExerciseLogEntity
import com.homestrength.data.local.entity.ExerciseSetEntity
import com.homestrength.data.local.entity.LightPracticeEntity
import com.homestrength.data.local.entity.PeriodGoalEntity
import com.homestrength.data.local.entity.PowerListItemEntity
import com.homestrength.data.local.entity.RewardEntity
import com.homestrength.data.local.entity.RewardRedemptionEntity
import com.homestrength.data.local.entity.SetBandCrossRef
import com.homestrength.data.local.entity.TraineeProfileEntity
import com.homestrength.data.local.entity.WorkoutSessionEntity
import com.homestrength.data.seed.SeedData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Database(
    entities = [
        ExerciseEntity::class,
        BandEntity::class,
        AppSettingsEntity::class,
        WorkoutSessionEntity::class,
        ExerciseLogEntity::class,
        ExerciseSetEntity::class,
        SetBandCrossRef::class,
        TraineeProfileEntity::class,
        PowerListItemEntity::class,
        LightPracticeEntity::class,
        RewardEntity::class,
        RewardRedemptionEntity::class,
        PeriodGoalEntity::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HomeStrengthDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun bandDao(): BandDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun traineeDao(): TraineeDao

    companion object {
        /** Keep in sync with [@Database] version for backup metadata. */
        const val SCHEMA_VERSION = 12

        @Volatile
        private var instance: HomeStrengthDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trainee_profile (
                        id INTEGER NOT NULL,
                        nickname TEXT NOT NULL,
                        fans INTEGER NOT NULL,
                        coins INTEGER NOT NULL,
                        energy INTEGER NOT NULL,
                        energyDate TEXT NOT NULL,
                        showCultivation INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS power_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        dayKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        track TEXT,
                        status TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        note TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_power_list_items_dayKey_sortOrder ON power_list_items(dayKey, sortOrder)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS light_practices (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        track TEXT NOT NULL,
                        startedAt INTEGER NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        note TEXT NOT NULL,
                        noteTitle TEXT NOT NULL,
                        completed INTEGER NOT NULL,
                        fansEarned INTEGER NOT NULL,
                        coinsEarned INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_light_practices_track_startedAt ON light_practices(track, startedAt)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rewards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        cost INTEGER NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rewards_sortOrder ON rewards(sortOrder)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reward_redemptions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        rewardId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        cost INTEGER NOT NULL,
                        redeemedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reward_redemptions_redeemedAt ON reward_redemptions(redeemedAt)"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE power_list_items ADD COLUMN rewardFans INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE power_list_items ADD COLUMN rewardCoins INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN checklistFans INTEGER NOT NULL DEFAULT 20")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN checklistCoins INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN lightFans INTEGER NOT NULL DEFAULT 40")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN lightCoins INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN strengthFans INTEGER NOT NULL DEFAULT 100")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN strengthCoins INTEGER NOT NULL DEFAULT 12")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN sleepEnergyRestore INTEGER NOT NULL DEFAULT 20")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN sleepFans INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN sleepCoins INTEGER NOT NULL DEFAULT 2")
                db.execSQL("ALTER TABLE trainee_profile ADD COLUMN sleepDate TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trainee_profile ADD COLUMN celebratedGrade TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "UPDATE trainee_profile SET nickname = 'W' WHERE nickname = '练习生' OR nickname = ''"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trainee_profile ADD COLUMN meditationEnergyRestore INTEGER NOT NULL DEFAULT 5"
                )
                db.execSQL(
                    "ALTER TABLE trainee_profile ADD COLUMN meditationFans INTEGER NOT NULL DEFAULT 5"
                )
                db.execSQL(
                    "ALTER TABLE trainee_profile ADD COLUMN meditationCoins INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "UPDATE light_practices SET track = 'CULTIVATION' WHERE track IN ('READING', 'CALLIGRAPHY')"
                )
                db.execSQL(
                    "UPDATE power_list_items SET track = 'CULTIVATION' WHERE track IN ('READING', 'CALLIGRAPHY')"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS month_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        monthKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        track TEXT,
                        status TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_month_goals_monthKey_sortOrder ON month_goals(monthKey, sortOrder)"
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS period_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        periodKind TEXT NOT NULL,
                        periodKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        track TEXT,
                        status TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_period_goals_periodKind_periodKey_sortOrder ON period_goals(periodKind, periodKey, sortOrder)"
                )
                db.execSQL(
                    """
                    INSERT INTO period_goals (id, periodKind, periodKey, title, track, status, sortOrder)
                    SELECT id, 'MONTH', monthKey, title, track, status, sortOrder FROM month_goals
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS month_goals")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trainee_profile ADD COLUMN coinsPerYuan INTEGER NOT NULL DEFAULT 10"
                )
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE period_goals ADD COLUMN targetParts INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE period_goals ADD COLUMN doneParts INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE power_list_items ADD COLUMN targetDurationSeconds INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE power_list_items ADD COLUMN practicedMinutes INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE power_list_items ADD COLUMN linkedPracticeId INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE power_list_items ADD COLUMN completedViaPractice INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .build()
        }

        private val seedMutex = Mutex()
    }

    suspend fun seedIfNeeded() {
        seedMutex.withLock {
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
            val profile = traineeDao().getProfile()
            if (profile == null) {
                traineeDao().upsertProfile(TraineeProfileEntity())
            } else if (profile.nickname == "练习生" || profile.nickname.isBlank()) {
                traineeDao().upsertProfile(profile.copy(nickname = "W"))
            }
            deduplicateExercises()
            deduplicateBands()
            clearAccidentalSkipsOnIncomplete()
        }
    }

    private suspend fun deduplicateExercises() {
        val exerciseDao = exerciseDao()
        val grouped = exerciseDao.getAllOrdered().groupBy { it.workoutType to it.sortOrder }
        grouped.values.forEach { group ->
            if (group.size <= 1) return@forEach
            val keep = group.minBy { it.id }
            group.filter { it.id != keep.id }.forEach { dup ->
                exerciseDao.reassignLogs(dup.id, keep.id)
                exerciseDao.deleteById(dup.id)
            }
        }
    }

    private suspend fun deduplicateBands() {
        val bandDao = bandDao()
        val grouped = bandDao.getAll().groupBy { it.resistance }
        grouped.values.forEach { group ->
            if (group.size <= 1) return@forEach
            val keep = group.minBy { it.id }
            val mergedQty = group.sumOf { it.quantity }
            val mergedEnabled = group.any { it.enabled }
            bandDao.update(keep.copy(quantity = mergedQty, enabled = mergedEnabled))
            group.filter { it.id != keep.id }.forEach { dup -> bandDao.delete(dup) }
        }
    }

    private suspend fun clearAccidentalSkipsOnIncomplete() {
        val sessionDao = workoutSessionDao()
        val incomplete = sessionDao.getIncomplete() ?: return
        val logs = sessionDao.getLogsForSession(incomplete.id)
        if (logs.isNotEmpty() && logs.all { it.skipped }) {
            logs.forEach { sessionDao.updateLog(it.copy(skipped = false)) }
        }
    }
}
