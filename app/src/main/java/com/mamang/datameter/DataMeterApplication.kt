package com.mamang.datameter

import android.app.Application
import com.mamang.datameter.di.AppContainer
import com.mamang.datameter.di.AppContainerImpl
import com.mamang.datameter.worker.NotificationHelper
import com.mamang.datameter.worker.QuotaCheckWorker

class DataMeterApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainerImpl(this)
        NotificationHelper.createNotificationChannel(this)
        try {
            QuotaCheckWorker.schedule(this)
        } catch (_: Exception) {
            // WorkManager may not be initialized in unit test or Robolectric environments
        }
    }
}
