package com.sleepsaver.app.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import kotlin.math.max

data class UsageEventRecord(
    val timestamp: Long,
    val eventType: Int,
    val packageName: String? = null
)

data class UnlockSession(
    val startAt: Long,
    val endAt: Long,
    val openAtQueryEnd: Boolean = false
) {
    val durationMillis: Long get() = (endAt - startAt).coerceAtLeast(0L)
}

data class UsageSummary(
    val available: Boolean,
    val preSleepPhoneMinutes: Int?,
    val postCheckInPhoneMinutes: Int?,
    val nightUnlockCount: Int?,
    val nightPhoneMinutes: Int?,
    val nightSessions: List<UnlockSession> = emptyList()
) {
    companion object {
        fun unavailable() = UsageSummary(
            available = false,
            preSleepPhoneMinutes = null,
            postCheckInPhoneMinutes = null,
            nightUnlockCount = null,
            nightPhoneMinutes = null
        )
    }
}

class UsageStatsAnalyzer(private val context: Context) {
    fun analyze(sleepCheckInAt: Long, wakeCheckInAt: Long): UsageSummary {
        if (!UsageAccess.isGranted(context)) return UsageSummary.unavailable()

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val queryStart = sleepCheckInAt - LOOKBACK_MILLIS
        val usageEvents = manager.queryEvents(queryStart, wakeCheckInAt)
        val records = buildList {
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                add(
                    UsageEventRecord(
                        timestamp = event.timeStamp,
                        eventType = event.eventType,
                        packageName = event.packageName
                    )
                )
            }
        }

        return summarize(
            events = records,
            sleepCheckInAt = sleepCheckInAt,
            wakeCheckInAt = wakeCheckInAt,
            sleepSaverPackage = context.packageName
        )
    }

    companion object {
        const val LOOKBACK_MILLIS = 12 * 60 * 60_000L

        fun summarize(
            events: List<UsageEventRecord>,
            sleepCheckInAt: Long,
            wakeCheckInAt: Long,
            sleepSaverPackage: String
        ): UsageSummary {
            val sorted = events.sortedBy { it.timestamp }
            val preSleepStart = sleepCheckInAt - 30 * 60_000L
            val preSleepMillis = foregroundDuration(
                events = sorted,
                rangeStart = preSleepStart,
                rangeEnd = sleepCheckInAt
            )

            val postCheckInMillis = postCheckInContinuedUse(
                events = sorted,
                sleepCheckInAt = sleepCheckInAt,
                wakeCheckInAt = wakeCheckInAt,
                sleepSaverPackage = sleepSaverPackage
            )

            val sessions = unlockSessions(sorted, sleepCheckInAt, wakeCheckInAt)
            val nightSessions = sessions.filterNot { it.openAtQueryEnd }

            return UsageSummary(
                available = true,
                preSleepPhoneMinutes = (preSleepMillis / 60_000L).toInt(),
                postCheckInPhoneMinutes = (postCheckInMillis / 60_000L).toInt(),
                nightUnlockCount = nightSessions.size,
                nightPhoneMinutes = (nightSessions.sumOf { it.durationMillis } / 60_000L).toInt(),
                nightSessions = nightSessions
            )
        }

        internal fun unlockSessions(
            events: List<UsageEventRecord>,
            rangeStart: Long,
            rangeEnd: Long
        ): List<UnlockSession> {
            val result = mutableListOf<UnlockSession>()
            var openStart: Long? = null

            events.asSequence()
                .filter { it.timestamp in rangeStart..rangeEnd }
                .forEach { event ->
                    when (event.eventType) {
                        UsageEvents.Event.KEYGUARD_HIDDEN -> {
                            if (openStart == null) openStart = event.timestamp
                        }

                        UsageEvents.Event.KEYGUARD_SHOWN,
                        UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                            openStart?.let { start ->
                                result += UnlockSession(start, event.timestamp)
                                openStart = null
                            }
                        }
                    }
                }

            openStart?.let { start ->
                result += UnlockSession(start, rangeEnd, openAtQueryEnd = true)
            }
            return result
        }

        internal fun foregroundDuration(
            events: List<UsageEventRecord>,
            rangeStart: Long,
            rangeEnd: Long
        ): Long {
            if (rangeEnd <= rangeStart) return 0
            val foregroundPackages = mutableSetOf<String>()
            var activeStart: Long? = null
            var total = 0L

            events.filter { it.timestamp <= rangeEnd }.forEach { event ->
                val packageName = event.packageName ?: return@forEach
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (foregroundPackages.isEmpty()) {
                            activeStart = max(event.timestamp, rangeStart)
                        }
                        foregroundPackages += packageName
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        foregroundPackages -= packageName
                        if (foregroundPackages.isEmpty()) {
                            val start = activeStart
                            if (start != null && event.timestamp > rangeStart) {
                                total += (event.timestamp.coerceAtMost(rangeEnd) - start)
                                    .coerceAtLeast(0L)
                            }
                            activeStart = null
                        }
                    }
                }
            }

            activeStart?.let { start -> total += (rangeEnd - start).coerceAtLeast(0L) }
            return total.coerceAtMost(rangeEnd - rangeStart)
        }

        internal fun postCheckInContinuedUse(
            events: List<UsageEventRecord>,
            sleepCheckInAt: Long,
            wakeCheckInAt: Long,
            sleepSaverPackage: String
        ): Long {
            val continuedAt = events.firstOrNull {
                it.timestamp >= sleepCheckInAt &&
                    it.timestamp <= wakeCheckInAt &&
                    it.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                    !it.packageName.isNullOrBlank() &&
                    it.packageName != sleepSaverPackage
            }?.timestamp ?: return 0L

            val stoppedAt = events.firstOrNull {
                it.timestamp >= continuedAt &&
                    (it.eventType == UsageEvents.Event.KEYGUARD_SHOWN ||
                        it.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE)
            }?.timestamp ?: wakeCheckInAt

            return (stoppedAt - continuedAt).coerceAtLeast(0L)
        }
    }
}

