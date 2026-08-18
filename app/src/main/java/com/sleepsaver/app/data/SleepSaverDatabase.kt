package com.sleepsaver.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

object PersistencePolicy {
    const val DATABASE_VERSION = 1
    const val ALLOW_DESTRUCTIVE_MIGRATION = false
    const val AUTO_BACKUP_ENABLED = false
}

@Database(
    entities = [SleepSessionEntity::class],
    version = PersistencePolicy.DATABASE_VERSION,
    exportSchema = true
)
abstract class SleepSaverDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
}
