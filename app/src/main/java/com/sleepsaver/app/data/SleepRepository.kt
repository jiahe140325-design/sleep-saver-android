package com.sleepsaver.app.data

import com.sleepsaver.app.usage.UsageSummary
import com.sleepsaver.app.domain.SessionPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

class SleepRepository(private val dao: SleepSessionDao) {
    private val writeMutex = Mutex()

    val activeSession: Flow<SleepSessionEntity?> = dao.observeActive()
    val sessions: Flow<List<SleepSessionEntity>> = dao.observeAll()

    suspend fun startSession(
        moodBeforeSleep: String?,
        tags: List<String>,
        settings: AppSettings,
        now: Long = System.currentTimeMillis()
    ): StartSessionResult = writeMutex.withLock {
        if (dao.getActiveOnce() != null) return@withLock StartSessionResult.ALREADY_ACTIVE

        dao.insert(
            SleepSessionEntity(
                sessionDate = SleepSessionEntity.sessionDate(now),
                sleepCheckInAt = now,
                plannedBedtimeHour = settings.bedtimeHour,
                plannedBedtimeMinute = settings.bedtimeMinute,
                plannedWakeHour = settings.wakeHour,
                plannedWakeMinute = settings.wakeMinute,
                moodBeforeSleep = moodBeforeSleep,
                tagsJson = JSONArray(tags).toString(),
                createdAt = now,
                updatedAt = now
            )
        )
        StartSessionResult.CREATED
    }

    suspend fun finishSession(
        moodAfterWake: String?,
        usageSummary: UsageSummary,
        now: Long = System.currentTimeMillis()
    ): Boolean = writeMutex.withLock {
        val active = dao.getActiveOnce() ?: return@withLock false
        dao.update(
            active.copy(
                wakeCheckInAt = now,
                preSleepPhoneMinutes = usageSummary.preSleepPhoneMinutes,
                postCheckInPhoneMinutes = usageSummary.postCheckInPhoneMinutes,
                nightUnlockCount = usageSummary.nightUnlockCount,
                nightPhoneMinutes = usageSummary.nightPhoneMinutes,
                moodAfterWake = moodAfterWake,
                usageDataAvailable = usageSummary.available,
                updatedAt = now
            )
        )
        true
    }

    suspend fun undoActiveSession(
        now: Long = System.currentTimeMillis(),
        undoWindowMillis: Long = 5 * 60_000L
    ): Boolean = writeMutex.withLock {
        val active = dao.getActiveOnce() ?: return@withLock false
        if (!SessionPolicy.canUndo(active, now, undoWindowMillis)) return@withLock false
        dao.delete(active)
        true
    }

    suspend fun allSessionsOnce(): List<SleepSessionEntity> = dao.getAllOnce()
}
