package com.nowlsantos.tongitsai

import android.app.Application
import com.nowlsantos.tongitsai.di.gameModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class TongitsAIApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@TongitsAIApplication)
            modules(gameModule)
        }
    }
}