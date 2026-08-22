package com.sleepsaver.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object PersistencePolicy {
    const val DATABASE_VERSION = 2
    const val ALLOW_DESTRUCTIVE_MIGRATION = false
    const val AUTO_BACKUP_ENABLED = false
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN plannedBedtimeHour INTEGER")
        database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN plannedBedtimeMinute INTEGER")
        database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN plannedWakeHour INTEGER")
        database.execSQL("ALTER TABLE sleep_sessions ADD COLUMN plannedWakeMinute INTEGER")
    }
}

@Database(
    entities = [SleepSessionEntity::class],
    version = PersistencePolicy.DATABASE_VERSION,
    exportSchema = true
)
abstract class SleepSaverDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
}
