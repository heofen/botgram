package com.heofen.botgram.di

import android.content.Context
import com.heofen.botgram.BotgramApplication

val Context.appContainer: AppContainer
    get() = (applicationContext as BotgramApplication).appContainer
