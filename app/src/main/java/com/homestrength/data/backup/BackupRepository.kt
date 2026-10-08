package com.homestrength.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.local.entity.AppSettingsEntity
import com.homestrength.data.local.entity.TraineeProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class BackupRepository(
    private val database: HomeStrengthDatabase
) {
    private val exerciseDao get() = database.exerciseDao()
    private val bandDao get() = database.bandDao()
    private val settingsDao get() = database.appSettingsDao()
    private val sessionDao get() = database.workoutSessionDao()
    private val traineeDao get() = database.traineeDao()

    suspend fun buildPayload(): BackupCodec.Payload = withContext(Dispatchers.IO) {
        BackupCodec.Payload(
            exportedAt = System.currentTimeMillis(),
            dbVersion = HomeStrengthDatabase.SCHEMA_VERSION,
            profile = traineeDao.getProfile() ?: TraineeProfileEntity(),
            settings = settingsDao.get() ?: AppSettingsEntity(),
            exercises = exerciseDao.getAllOrdered(),
            bands = bandDao.getAll(),
            sessions = sessionDao.getAllSessions(),
            logs = sessionDao.getAllLogs(),
            sets = sessionDao.getAllSets(),
            setBands = sessionDao.getAllSetBands(),
            powerList = traineeDao.getAllPowerItems(),
            lightPractices = traineeDao.getAllLightPractices(),
            periodGoals = traineeDao.getAllPeriodGoals(),
            rewards = traineeDao.getRewards(),
            redemptions = traineeDao.getAllRedemptions()
        )
    }

    suspend fun exportToUri(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val json = BackupCodec.encode(buildPayload())
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: error("无法写入文件")
        }
    }

    suspend fun importFromUri(context: Context, uri: Uri): Result<BackupCodec.Payload> =
        withContext(Dispatchers.IO) {
            runCatching {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
                } ?: error("无法读取文件")
                val payload = BackupCodec.decode(json)
                replaceAll(payload)
                payload
            }
        }

    /** Replace local DB with [payload]. Used by import and unit tests. */
    suspend fun replaceAll(payload: BackupCodec.Payload) = withContext(Dispatchers.IO) {
        database.withTransaction {
            clearUserData()
            insertAll(payload)
        }
        // After restore, reset AUTOINCREMENT so new rows don't collide with restored ids.
        runCatching {
            database.openHelper.writableDatabase.execSQL("DELETE FROM sqlite_sequence")
        }
    }

    private suspend fun clearUserData() {
        sessionDao.deleteAllSetBands()
        sessionDao.deleteAllSets()
        sessionDao.deleteAllLogs()
        sessionDao.deleteAllSessions()
        traineeDao.deleteAllPowerItems()
        traineeDao.deleteAllLightPractices()
        traineeDao.deleteAllPeriodGoals()
        traineeDao.deleteAllRedemptions()
        traineeDao.deleteAllRewards()
        exerciseDao.deleteAll()
        bandDao.deleteAll()
    }

    private suspend fun insertAll(payload: BackupCodec.Payload) {
        if (payload.exercises.isNotEmpty()) exerciseDao.insertAll(payload.exercises)
        if (payload.bands.isNotEmpty()) bandDao.insertAll(payload.bands)
        settingsDao.upsert(payload.settings)
        traineeDao.upsertProfile(payload.profile)
        if (payload.rewards.isNotEmpty()) traineeDao.insertRewards(payload.rewards)
        if (payload.redemptions.isNotEmpty()) traineeDao.insertRedemptions(payload.redemptions)
        if (payload.periodGoals.isNotEmpty()) traineeDao.insertPeriodGoals(payload.periodGoals)
        if (payload.lightPractices.isNotEmpty()) traineeDao.insertLightPractices(payload.lightPractices)
        if (payload.powerList.isNotEmpty()) traineeDao.insertPowerItems(payload.powerList)
        if (payload.sessions.isNotEmpty()) sessionDao.insertSessions(payload.sessions)
        if (payload.logs.isNotEmpty()) sessionDao.insertLogsReplace(payload.logs)
        if (payload.sets.isNotEmpty()) sessionDao.insertSets(payload.sets)
        if (payload.setBands.isNotEmpty()) sessionDao.insertSetBands(payload.setBands)
    }
}
