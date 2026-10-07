package com.homestrength.di

import android.content.Context
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.repository.HomeStrengthRepository
import com.homestrength.data.repository.TraineeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class AppContainer(context: Context) {
    private val database = HomeStrengthDatabase.getInstance(context.applicationContext)

    val traineeRepository = TraineeRepository(
        traineeDao = database.traineeDao()
    )

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
}
