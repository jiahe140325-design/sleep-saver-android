package com.sleepsaver.app

import android.app.Application
import androidx.room.Room
import com.sleepsaver.app.data.SettingsRepository
import com.sleepsaver.app.data.SleepRepository
import com.sleepsaver.app.data.SleepSaverDatabase
import com.sleepsaver.app.usage.UsageStatsAnalyzer

class SleepSaverApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(
        application,
        SleepSaverDatabase::class.java,
        "sleep-saver.db"
    ).build()

    val sleepRepository = SleepRepository(database.sleepSessionDao())
    val settingsRepository = SettingsRepository(application)
    val usageStatsAnalyzer = UsageStatsAnalyzer(application)
}

