package com.hisabbondhu.app

import android.app.Application
import com.hisabbondhu.app.sync.Notifications

class HisabApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        container.scheduler.schedulePeriodic()
    }
}
