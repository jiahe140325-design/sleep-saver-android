package com.sleepsaver.app.data

import com.sleepsaver.app.domain.SessionPolicy
import com.sleepsaver.app.domain.SessionTimePolicy
import com.sleepsaver.app.domain.SessionTimeValidation
import com.sleepsaver.app.usage.UsageSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray

internal const val DISPLAY_MINUTE_MILLIS = 60_000L

internal fun resolveCheckInTime(selectedAt: Long, referenceAt: Long, wasAdjusted: Boolean): Long {
    if (!wasAdjusted) return referenceAt
    return if (selectedAt / DISPLAY_MINUTE_MILLIS == referenceAt / DISPLAY_MINUTE_MILLIS) {
        referenceAt
    } else {
        selectedAt
    }
}

class SleepRepository(private val dao: SleepSessionDao) {
    private val writeMutex = Mutex()

    val activeSession: Flow<SleepSessionEntity?> = dao.observeActive()
    val sessions: Flow<List<SleepSessionEntity>> = dao.observeAll()

    suspend fun startSession(
        moodBeforeSleep: String?,
        tags: List<String>,
        settings: AppSettings,
        effectiveSleepAt: Long = System.currentTimeMillis(),
        timeWasAdjusted: Boolean = false,
        recordedAt: Long = System.currentTimeMillis()
    ): StartSessionResult = writeMutex.withLock {
        if (dao.getActiveOnce() != null) return@withLock StartSessionResult.ALREADY_ACTIVE
        if (effectiveSleepAt > recordedAt) return@withLock StartSessionResult.INVALID_TIME
        val storedSleepAt = resolveCheckInTime(effectiveSleepAt, recordedAt, timeWasAdjusted)

        dao.insert(
            SleepSessionEntity(
                sessionDate = SleepSessionEntity.sessionDate(storedSleepAt),
                sleepCheckInAt = storedSleepAt,
                plannedBedtimeHour = settings.bedtimeHour,
                plannedBedtimeMinute = settings.bedtimeMinute,
                plannedWakeHour = settings.wakeHour,
                plannedWakeMinute = settings.wakeMinute,
                originalSleepCheckInAt = recordedAt.takeIf { it != storedSleepAt },
                correctedAt = recordedAt.takeIf { it != storedSleepAt },
                moodBeforeSleep = moodBeforeSleep,
                tagsJson = JSONArray(tags).toString(),
                createdAt = recordedAt,
                updatedAt = recordedAt
            )
        )
        StartSessionResult.CREATED
    }

    suspend fun finishSession(
        moodAfterWake: String?,
        usageSummary: UsageSummary,
        effectiveSleepAt: Long,
        effectiveWakeAt: Long,
        sleepWasAdjusted: Boolean = false,
        wakeWasAdjusted: Boolean = false,
        recordedAt: Long = System.currentTimeMillis()
    ): SessionEditResult = writeMutex.withLock {
        val active = dao.getActiveOnce() ?: return@withLock SessionEditResult.NOT_FOUND
        val storedSleepAt = resolveCheckInTime(effectiveSleepAt, active.sleepCheckInAt, sleepWasAdjusted)
        val storedWakeAt = resolveCheckInTime(effectiveWakeAt, recordedAt, wakeWasAdjusted)
        validationResult(
            sleepAt = storedSleepAt,
            wakeAt = storedWakeAt,
            now = recordedAt,
            excludedId = active.id
        )?.let { return@withLock it }
        val sleepChanged = storedSleepAt != active.sleepCheckInAt
        val wakeChanged = storedWakeAt != recordedAt
        dao.update(
            active.copy(
                sessionDate = SleepSessionEntity.sessionDate(storedSleepAt),
                sleepCheckInAt = storedSleepAt,
                wakeCheckInAt = storedWakeAt,
                originalSleepCheckInAt = active.originalSleepCheckInAt
                    ?: active.sleepCheckInAt.takeIf { sleepChanged },
                originalWakeCheckInAt = active.originalWakeCheckInAt
                    ?: recordedAt.takeIf { wakeChanged },
                correctedAt = if (sleepChanged || wakeChanged) recordedAt else active.correctedAt,
                preSleepPhoneMinutes = usageSummary.preSleepPhoneMinutes,
                postCheckInPhoneMinutes = usageSummary.postCheckInPhoneMinutes,
                nightUnlockCount = usageSummary.nightUnlockCount,
                nightPhoneMinutes = usageSummary.nightPhoneMinutes,
                moodAfterWake = moodAfterWake,
                usageDataAvailable = usageSummary.available,
                updatedAt = recordedAt
            )
        )
        SessionEditResult.SAVED
    }

    suspend fun supplementSession(
        sleepAt: Long,
        wakeAt: Long,
        settings: AppSettings,
        usageSummary: UsageSummary,
        recordedAt: Long = System.currentTimeMillis()
    ): SessionEditResult = writeMutex.withLock {
        validationResult(sleepAt, wakeAt, recordedAt)?.let { return@withLock it }
        dao.insert(
            SleepSessionEntity(
                sessionDate = SleepSessionEntity.sessionDate(sleepAt),
                sleepCheckInAt = sleepAt,
                wakeCheckInAt = wakeAt,
                plannedBedtimeHour = settings.bedtimeHour,
                plannedBedtimeMinute = settings.bedtimeMinute,
                plannedWakeHour = settings.wakeHour,
                plannedWakeMinute = settings.wakeMinute,
                preSleepPhoneMinutes = usageSummary.preSleepPhoneMinutes,
                postCheckInPhoneMinutes = usageSummary.postCheckInPhoneMinutes,
                nightUnlockCount = usageSummary.nightUnlockCount,
                nightPhoneMinutes = usageSummary.nightPhoneMinutes,
                isSupplemented = true,
                usageDataAvailable = usageSummary.available,
                createdAt = recordedAt,
                updatedAt = recordedAt
            )
        )
        SessionEditResult.SAVED
    }

    suspend fun correctSession(
        id: Long,
        sleepAt: Long,
        wakeAt: Long,
        usageSummary: UsageSummary,
        correctedAt: Long = System.currentTimeMillis()
    ): SessionEditResult = writeMutex.withLock {
        val session = dao.getById(id) ?: return@withLock SessionEditResult.NOT_FOUND
        val currentWakeAt = session.wakeCheckInAt ?: return@withLock SessionEditResult.NOT_FOUND
        val storedSleepAt = resolveCheckInTime(sleepAt, session.sleepCheckInAt, wasAdjusted = true)
        val storedWakeAt = resolveCheckInTime(wakeAt, currentWakeAt, wasAdjusted = true)
        validationResult(storedSleepAt, storedWakeAt, correctedAt, id)?.let { return@withLock it }
        val sleepChanged = storedSleepAt != session.sleepCheckInAt
        val wakeChanged = storedWakeAt != currentWakeAt
        dao.update(
            session.copy(
                sessionDate = SleepSessionEntity.sessionDate(storedSleepAt),
                sleepCheckInAt = storedSleepAt,
                wakeCheckInAt = storedWakeAt,
                originalSleepCheckInAt = session.originalSleepCheckInAt
                    ?: session.sleepCheckInAt.takeIf { sleepChanged },
                originalWakeCheckInAt = session.originalWakeCheckInAt
                    ?: currentWakeAt.takeIf { wakeChanged },
                correctedAt = if (sleepChanged || wakeChanged) correctedAt else session.correctedAt,
                preSleepPhoneMinutes = usageSummary.preSleepPhoneMinutes,
                postCheckInPhoneMinutes = usageSummary.postCheckInPhoneMinutes,
                nightUnlockCount = usageSummary.nightUnlockCount,
                nightPhoneMinutes = usageSummary.nightPhoneMinutes,
                usageDataAvailable = usageSummary.available,
                updatedAt = correctedAt
            )
        )
        SessionEditResult.SAVED
    }

    suspend fun restoreOriginalSession(
        id: Long,
        usageSummary: UsageSummary,
        restoredAt: Long = System.currentTimeMillis()
    ): SessionEditResult = writeMutex.withLock {
        val session = dao.getById(id) ?: return@withLock SessionEditResult.NOT_FOUND
        val sleepAt = session.originalSleepCheckInAt ?: session.sleepCheckInAt
        val wakeAt = session.originalWakeCheckInAt ?: session.wakeCheckInAt
            ?: return@withLock SessionEditResult.NOT_FOUND
        validationResult(sleepAt, wakeAt, restoredAt, id)?.let { return@withLock it }
        dao.update(
            session.copy(
                sessionDate = SleepSessionEntity.sessionDate(sleepAt),
                sleepCheckInAt = sleepAt,
                wakeCheckInAt = wakeAt,
                originalSleepCheckInAt = null,
                originalWakeCheckInAt = null,
                correctedAt = null,
                preSleepPhoneMinutes = usageSummary.preSleepPhoneMinutes,
                postCheckInPhoneMinutes = usageSummary.postCheckInPhoneMinutes,
                nightUnlockCount = usageSummary.nightUnlockCount,
                nightPhoneMinutes = usageSummary.nightPhoneMinutes,
                usageDataAvailable = usageSummary.available,
                updatedAt = restoredAt
            )
        )
        SessionEditResult.SAVED
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

    private suspend fun validationResult(
        sleepAt: Long,
        wakeAt: Long,
        now: Long,
        excludedId: Long? = null
    ): SessionEditResult? {
        val others = dao.getAllOnce().filterNot { it.id == excludedId }
        return when (SessionTimePolicy.validate(sleepAt, wakeAt, now, others)) {
            SessionTimeValidation.VALID -> null
            SessionTimeValidation.INVALID_ORDER -> SessionEditResult.INVALID_ORDER
            SessionTimeValidation.FUTURE_TIME -> SessionEditResult.FUTURE_TIME
            SessionTimeValidation.OVERLAP -> SessionEditResult.OVERLAP
        }
    }
}
