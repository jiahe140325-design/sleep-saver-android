package com.sleepsaver.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions WHERE wakeCheckInAt IS NULL ORDER BY sleepCheckInAt DESC LIMIT 1")
    fun observeActive(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE wakeCheckInAt IS NULL ORDER BY sleepCheckInAt DESC LIMIT 1")
    suspend fun getActiveOnce(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions ORDER BY sleepCheckInAt DESC")
    fun observeAll(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions ORDER BY sleepCheckInAt DESC")
    suspend fun getAllOnce(): List<SleepSessionEntity>

    @Insert
    suspend fun insert(session: SleepSessionEntity): Long

    @Update
    suspend fun update(session: SleepSessionEntity)

    @Delete
    suspend fun delete(session: SleepSessionEntity)
}

