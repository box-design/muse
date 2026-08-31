package com.muse.app

import android.app.Application
import com.muse.app.di.AppContainer

class MuseApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
