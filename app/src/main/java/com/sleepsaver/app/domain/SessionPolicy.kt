package com.sleepsaver.app.domain

import com.sleepsaver.app.data.SleepSessionEntity

enum class SessionState {
    NO_SESSION,
    SLEEP_SESSION_ACTIVE
}

object SessionPolicy {
    const val UNDO_WINDOW_MILLIS = 5 * 60_000L

    fun stateOf(activeSession: SleepSessionEntity?): SessionState =
        if (activeSession == null) SessionState.NO_SESSION else SessionState.SLEEP_SESSION_ACTIVE

    fun canStart(activeSession: SleepSessionEntity?): Boolean = activeSession == null

    fun canWake(activeSession: SleepSessionEntity?): Boolean = activeSession != null

    fun canUndo(
        activeSession: SleepSessionEntity?,
        now: Long,
        undoWindowMillis: Long = UNDO_WINDOW_MILLIS
    ): Boolean = activeSession != null &&
        now >= (activeSession.originalSleepCheckInAt ?: activeSession.createdAt) &&
        now - (activeSession.originalSleepCheckInAt ?: activeSession.createdAt) <= undoWindowMillis
}

