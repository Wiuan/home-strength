package com.homestrength

import android.app.Application
import com.homestrength.di.AppContainer

class HomeStrengthApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
