package com.heofen.botgram.di

import android.content.Context
import com.heofen.botgram.BotgramApplication

/** Даёт быстрый доступ к `AppContainer` из любого `Context`. */
val Context.appContainer: AppContainer
    get() = (applicationContext as BotgramApplication).appContainer
