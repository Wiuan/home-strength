package com.homestrength.di

import android.content.Context
import com.homestrength.data.backup.BackupRepository
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import com.homestrength.data.room.RoomPrefs
import com.homestrength.data.room.RoomSyncRepository
import com.homestrength.data.track.LightTrackCatalog
import com.homestrength.data.update.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val database = HomeStrengthDatabase.getInstance(appContext)

    val roomPrefs = RoomPrefs(appContext)

    val lightTrackCatalog = LightTrackCatalog(appContext)

    val traineeRepository = TraineeRepository(
        traineeDao = database.traineeDao(),
        lightTrackCatalog = lightTrackCatalog
    )

    val backupRepository = BackupRepository(database)

    init {
        runBlocking(Dispatchers.IO) {
            database.seedIfNeeded()
            traineeRepository.seedDefaultRewardsIfEmpty()
        }
    }

    val repository = HomeStrengthRepository(
        exerciseDao = database.exerciseDao(),
        bandDao = database.bandDao(),
        settingsDao = database.appSettingsDao(),
        sessionDao = database.workoutSessionDao(),
        traineeRepository = traineeRepository
    )

    val roomSyncRepository = RoomSyncRepository(
        prefs = roomPrefs,
        traineeRepository = traineeRepository,
        homeRepository = repository
    )

    val appUpdateRepository = AppUpdateRepository(
        context = appContext,
        prefs = roomPrefs
    )
}
