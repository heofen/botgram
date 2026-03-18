package com.heofen.botgram

import android.app.Application
import com.heofen.botgram.di.AppContainer
import com.heofen.botgram.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Точка входа приложения.
 *
 * Создаёт корневой контейнер зависимостей и запускает Koin.
 */
class BotgramApplication : Application() {
    /** Общий контейнер приложения с лениво создаваемыми зависимостями. */
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)

        startKoin {
            androidContext(this@BotgramApplication)
            modules(appModule(appContainer))
        }
    }
}
