package com.sleepsaver.app.domain

import com.sleepsaver.app.data.AppSettings
import com.sleepsaver.app.data.SleepSessionEntity
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

enum class CheckInPhase {
    BEDTIME,
    WAKE,
    MISSING_PREVIOUS,
    DAY_COMPLETE
}

data class PlannedSleepWindow(
    val bedtimeAt: Long,
    val wakeAt: Long,
    val nextBedtimeAt: Long
)

object CheckInPhasePolicy {
    fun phase(
        activeSession: SleepSessionEntity?,
        completedSessions: List<SleepSessionEntity>,
        settings: AppSettings,
        now: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): CheckInPhase {
        if (activeSession != null) return CheckInPhase.WAKE

        val window = windowAt(now, settings, zoneId)
        if (now < window.wakeAt) return CheckInPhase.BEDTIME

        val completedInWindow = completedSessions.any { session ->
            val wake = session.wakeCheckInAt ?: return@any false
            wake >= window.bedtimeAt && wake < window.nextBedtimeAt
        }
        if (completedInWindow) return CheckInPhase.DAY_COMPLETE

        return CheckInPhase.MISSING_PREVIOUS
    }

    fun hasPreviousMissing(
        completedSessions: List<SleepSessionEntity>,
        settings: AppSettings,
        now: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val previousWindow = previousWindowAt(now, settings, zoneId)
        return completedSessions.none { session ->
            val wake = session.wakeCheckInAt ?: return@none false
            wake >= previousWindow.bedtimeAt && wake < previousWindow.nextBedtimeAt
        }
    }

    fun previousWindowAt(
        now: Long,
        settings: AppSettings,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PlannedSleepWindow = windowAt(now - DAY_MILLIS, settings, zoneId)

    fun windowAt(
        now: Long,
        settings: AppSettings,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PlannedSleepWindow {
        val current = Instant.ofEpochMilli(now).atZone(zoneId)
        val bedtime = LocalTime.of(settings.bedtimeHour, settings.bedtimeMinute)
        val wake = LocalTime.of(settings.wakeHour, settings.wakeMinute)
        val bedtimeDate = if (!current.toLocalTime().isBefore(bedtime)) {
            current.toLocalDate()
        } else {
            current.toLocalDate().minusDays(1)
        }
        val bedtimeAt = bedtimeDate.atTime(bedtime).atZone(zoneId)
        val wakeDate = if (wake.isAfter(bedtime)) bedtimeDate else bedtimeDate.plusDays(1)
        val wakeAt = wakeDate.atTime(wake).atZone(zoneId)
        val nextBedtimeAt = bedtimeDate.plusDays(1).atTime(bedtime).atZone(zoneId)
        return PlannedSleepWindow(
            bedtimeAt = bedtimeAt.toInstant().toEpochMilli(),
            wakeAt = wakeAt.toInstant().toEpochMilli(),
            nextBedtimeAt = nextBedtimeAt.toInstant().toEpochMilli()
        )
    }

    private const val DAY_MILLIS = 24 * 60 * 60_000L
}

enum class SessionTimeValidation {
    VALID,
    INVALID_ORDER,
    FUTURE_TIME,
    OVERLAP
}

object SessionTimePolicy {
    fun validate(
        sleepAt: Long,
        wakeAt: Long,
        now: Long,
        otherSessions: List<SleepSessionEntity>
    ): SessionTimeValidation {
        if (wakeAt <= sleepAt) return SessionTimeValidation.INVALID_ORDER
        if (sleepAt > now || wakeAt > now) return SessionTimeValidation.FUTURE_TIME
        val overlaps = otherSessions.any { other ->
            val otherWake = other.wakeCheckInAt ?: return@any false
            sleepAt < otherWake && wakeAt > other.sleepCheckInAt
        }
        return if (overlaps) SessionTimeValidation.OVERLAP else SessionTimeValidation.VALID
    }
}
