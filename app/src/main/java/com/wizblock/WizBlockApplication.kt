package com.wizblock

import android.app.Application

class WizBlockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
