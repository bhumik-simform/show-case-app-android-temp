package com.example.recipeapp

import android.app.Application
import com.example.recipeapp.core.di.appModule
import com.example.recipeapp.core.network.ConnectivityChecker
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

import com.example.recipeapp.core.notifications.NotificationChannels

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        ConnectivityChecker.init(this)
        NotificationChannels.ensureCreated(this)
        startKoin {
            androidContext(this@App)
            modules(appModule)
        }
    }
}
