package com.alexrcq.tvpicturesettings

import android.app.Application
import timber.log.Timber

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.BUILD_TYPE == "debugMinified") {
            Timber.plant(Timber.DebugTree())
        }
    }
}