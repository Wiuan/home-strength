package com.homestrength.di

import android.content.Context
import com.homestrength.data.local.db.HomeStrengthDatabase
import com.homestrength.data.repository.HomeStrengthRepository

class AppContainer(context: Context) {
    private val database = HomeStrengthDatabase.getInstance(context)

    val repository = HomeStrengthRepository(
        exerciseDao = database.exerciseDao(),
        bandDao = database.bandDao(),
        settingsDao = database.appSettingsDao(),
        sessionDao = database.workoutSessionDao()
    )
}
