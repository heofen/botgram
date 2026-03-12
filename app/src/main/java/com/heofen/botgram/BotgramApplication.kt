package com.heofen.botgram

import android.app.Application
import com.heofen.botgram.di.AppContainer

class BotgramApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
