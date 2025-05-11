package com.devx.signbridge

import android.app.Application
import com.devx.signbridge.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SignBridgeApp: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SignBridgeApp)
            modules(appModule)
        }
    }
}