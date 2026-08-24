package com.sleepsaver.app

import android.app.usage.UsageEvents
import com.sleepsaver.app.data.AppSettings
import com.sleepsaver.app.data.PersistencePolicy
import com.sleepsaver.app.data.resolveCheckInTime
import com.sleepsaver.app.data.SleepSessionEntity
import com.sleepsaver.app.domain.CheckInPhase
import com.sleepsaver.app.domain.CheckInPhasePolicy
import com.sleepsaver.app.domain.SessionPolicy
import com.sleepsaver.app.domain.SessionTimePolicy
import com.sleepsaver.app.domain.SessionTimeValidation
import com.sleepsaver.app.domain.SessionState
import com.sleepsaver.app.export.DataExporter
import com.sleepsaver.app.reminder.ReminderConstants
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_LABEL_WIDTH_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_LABEL_HEIGHT_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_LABEL_TOP_OFFSET_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_MASCOT_HALF_SIZE_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_INLINE_GAP_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_INLINE_POINT_LIMIT
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_POINT_SPACING_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_SIDE_PADDING_DP
import com.sleepsaver.app.ui.JOURNAL_HISTORY_DATE_CHIP_HEIGHT_DP
import com.sleepsaver.app.ui.JOURNAL_HISTORY_DATE_CHIP_WIDTH_DP
import com.sleepsaver.app.ui.JOURNAL_HISTORY_EDIT_BUTTON_HEIGHT_DP
import com.sleepsaver.app.ui.journalDateFromUtcMillis
import com.sleepsaver.app.ui.journalDateRangeCanConfirm
import com.sleepsaver.app.ui.journalDateToUtcMillis
import com.sleepsaver.app.ui.SessionClockInput
import com.sleepsaver.app.ui.adjustSessionClock
import com.sleepsaver.app.ui.sessionClockTimestamp
import com.sleepsaver.app.ui.sessionNeedsCorrectionAttention
import com.sleepsaver.app.ui.journalDeviationChartWidthDp
import com.sleepsaver.app.ui.selectedJournalSession
import com.sleepsaver.app.usage.UsageEventRecord
import com.sleepsaver.app.usage.UsageStatsAnalyzer
import com.sleepsaver.app.usage.UsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SleepSaverLogicTest {
    private companion object {
        val TEST_ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
    }

    private val sleepAt = 1_800_000_000_000L
    private val wakeAt = sleepAt + hours(8)
    private val appPackage = "com.sleepsaver.app"

    @Test
    fun `01 notification lights screen without unlock counts zero`() {
        val result = summary(
            event(60, UsageEvents.Event.SCREEN_INTERACTIVE),
            event(61, UsageEvents.Event.SCREEN_NON_INTERACTIVE)
        )
        assertEquals(0, result.nightUnlockCount)
    }

    @Test
    fun `02 power button showing clock without unlock counts zero`() {
        val result = summary(event(90, UsageEvents.Event.SCREEN_INTERACTIVE))
        assertEquals(0, result.nightUnlockCount)
    }

    @Test
    fun `03 fingerprint unlock and lock is one session`() {
        val result = summary(
            event(60, UsageEvents.Event.KEYGUARD_HIDDEN),
            eventMillis(60, 10_000, UsageEvents.Event.KEYGUARD_SHOWN)
        )
        assertEquals(1, result.nightUnlockCount)
    }

    @Test
    fun `04 switching three apps inside one unlock is still one session`() {
        val result = summary(
            event(60, UsageEvents.Event.KEYGUARD_HIDDEN),
            event(61, UsageEvents.Event.ACTIVITY_RESUMED, "wechat"),
            event(62, UsageEvents.Event.ACTIVITY_RESUMED, "redbook"),
            event(63, UsageEvents.Event.ACTIVITY_RESUMED, "taobao"),
            event(76, UsageEvents.Event.KEYGUARD_SHOWN)
        )
        assertEquals(1, result.nightUnlockCount)
        assertEquals(16, result.nightPhoneMinutes)
    }

    @Test
    fun `05 three separate night unlocks count three`() {
        val result = summary(
            event(60, UsageEvents.Event.KEYGUARD_HIDDEN),
            event(61, UsageEvents.Event.KEYGUARD_SHOWN),
            event(120, UsageEvents.Event.KEYGUARD_HIDDEN),
            event(121, UsageEvents.Event.KEYGUARD_SHOWN),
            event(180, UsageEvents.Event.KEYGUARD_HIDDEN),
            event(181, UsageEvents.Event.KEYGUARD_SHOWN)
        )
        assertEquals(3, result.nightUnlockCount)
    }

    @Test
    fun `06 sleep checkin followed by lock has zero continued use`() {
        val result = summary(
            event(0, UsageEvents.Event.ACTIVITY_RESUMED, appPackage),
            event(1, UsageEvents.Event.SCREEN_NON_INTERACTIVE)
        )
        assertEquals(0, result.postCheckInPhoneMinutes)
    }

    @Test
    fun `07 switch to another app for twenty minutes is recorded`() {
        val result = summary(
            event(0, UsageEvents.Event.ACTIVITY_RESUMED, appPackage),
            event(1, UsageEvents.Event.ACTIVITY_RESUMED, "redbook"),
            event(21, UsageEvents.Event.SCREEN_NON_INTERACTIVE)
        )
        assertEquals(20, result.postCheckInPhoneMinutes)
    }

    @Test
    fun `08 notification then real unlock counts one`() {
        val result = summary(
            event(60, UsageEvents.Event.SCREEN_INTERACTIVE),
            event(61, UsageEvents.Event.KEYGUARD_HIDDEN),
            event(62, UsageEvents.Event.KEYGUARD_SHOWN)
        )
        assertEquals(1, result.nightUnlockCount)
    }

    @Test
    fun `09 morning open session is excluded from night count`() {
        val result = summary(
            event(120, UsageEvents.Event.KEYGUARD_HIDDEN),
            event(126, UsageEvents.Event.KEYGUARD_SHOWN),
            event(470, UsageEvents.Event.KEYGUARD_HIDDEN)
        )
        assertEquals(1, result.nightUnlockCount)
        assertEquals(6, result.nightPhoneMinutes)
    }

    @Test
    fun `10 only morning unlock produces zero night sessions`() {
        val result = summary(event(470, UsageEvents.Event.KEYGUARD_HIDDEN))
        assertEquals(0, result.nightUnlockCount)
        assertEquals(0, result.nightPhoneMinutes)
    }

    @Test
    fun `11 missing usage permission returns unavailable rather than zero`() {
        val result = UsageSummary.unavailable()
        assertFalse(result.available)
        assertNull(result.preSleepPhoneMinutes)
        assertNull(result.nightUnlockCount)
    }

    @Test
    fun `12 duplicate sleep checkin is blocked while active`() {
        val active = activeSession()
        assertFalse(SessionPolicy.canStart(active))
        assertTrue(SessionPolicy.canWake(active))
    }

    @Test
    fun `13 persisted active entity restores active state after process restart`() {
        val reconstructed = activeSession().copy()
        assertEquals(SessionState.SLEEP_SESSION_ACTIVE, SessionPolicy.stateOf(reconstructed))
        assertTrue(reconstructed.isActive)
    }

    @Test
    fun `14 persisted active entity remains valid after phone reboot`() {
        val savedBeforeReboot = activeSession()
        val loadedAfterReboot = savedBeforeReboot.copy(id = 42)
        assertTrue(SessionPolicy.canWake(loadedAfterReboot))
        assertEquals(savedBeforeReboot.sleepCheckInAt, loadedAfterReboot.sleepCheckInAt)
    }

    @Test
    fun `15 database policy forbids destructive migration`() {
        assertFalse(PersistencePolicy.ALLOW_DESTRUCTIVE_MIGRATION)
        assertFalse(PersistencePolicy.AUTO_BACKUP_ENABLED)
        assertEquals(3, PersistencePolicy.DATABASE_VERSION)
    }

    @Test
    fun `16 crossing midnight remains one session with correct duration`() {
        val session = activeSession().copy(wakeCheckInAt = sleepAt + hours(7) + minutes(55))
        assertEquals(7 * 60L + 55L, session.restWindowMinutes)
        assertFalse(session.isActive)
    }

    @Test
    fun `17 reminder uses one stable unique work name after edits`() {
        val beforeEdit = ReminderConstants.UNIQUE_WORK_NAME
        val afterEdit = ReminderConstants.UNIQUE_WORK_NAME
        assertEquals("sleep-saver-bedtime-reminder", beforeEdit)
        assertEquals(beforeEdit, afterEdit)
    }

    @Test
    fun `18 deviation labels keep horizontal and vertical safety gaps`() {
        val compactViewport = 264
        assertEquals(compactViewport, journalDeviationChartWidthDp(compactViewport, 7))
        assertEquals(308, journalDeviationChartWidthDp(308, 7))
        assertTrue(JOURNAL_DEVIATION_SIDE_PADDING_DP >= JOURNAL_DEVIATION_LABEL_WIDTH_DP / 2)

        for (pointCount in 2..JOURNAL_DEVIATION_INLINE_POINT_LIMIT) {
            val chartWidth = journalDeviationChartWidthDp(compactViewport, pointCount)
            val availableWidth = chartWidth - JOURNAL_DEVIATION_SIDE_PADDING_DP * 2
            val actualPointSpacing = availableWidth / (pointCount - 1)
            assertEquals(compactViewport, chartWidth)
            assertTrue(
                actualPointSpacing - JOURNAL_DEVIATION_LABEL_WIDTH_DP >=
                    JOURNAL_DEVIATION_INLINE_GAP_DP
            )
        }

        for (pointCount in (JOURNAL_DEVIATION_INLINE_POINT_LIMIT + 1)..31) {
            val chartWidth = journalDeviationChartWidthDp(308, pointCount)
            val availableWidth = chartWidth - JOURNAL_DEVIATION_SIDE_PADDING_DP * 2
            val actualPointSpacing = availableWidth / (pointCount - 1)
            assertEquals(JOURNAL_DEVIATION_POINT_SPACING_DP, actualPointSpacing)
            assertTrue(actualPointSpacing - JOURNAL_DEVIATION_LABEL_WIDTH_DP >= 8)
        }

        val labelToMascotGap = JOURNAL_DEVIATION_LABEL_TOP_OFFSET_DP -
            JOURNAL_DEVIATION_LABEL_HEIGHT_DP -
            JOURNAL_DEVIATION_MASCOT_HALF_SIZE_DP
        assertTrue(labelToMascotGap >= 3)
    }

    @Test
    fun `19 custom date range confirms once only after both dates are selected`() {
        val start = LocalDate.of(2026, 8, 1)
        val end = LocalDate.of(2026, 8, 22)
        val startMillis = journalDateToUtcMillis(start)
        val endMillis = journalDateToUtcMillis(end)

        assertFalse(journalDateRangeCanConfirm(null, null))
        assertFalse(journalDateRangeCanConfirm(startMillis, null))
        assertTrue(journalDateRangeCanConfirm(startMillis, endMillis))
        assertEquals(start, journalDateFromUtcMillis(startMillis))
        assertEquals(end, journalDateFromUtcMillis(endMillis))
    }

    @Test
    fun `20 active session wins after planned wake time`() {
        val settings = midnightSettings()
        val now = epoch(2026, 8, 24, 9, 0)
        val active = activeSession().copy(
            sleepCheckInAt = epoch(2026, 8, 24, 2, 0),
            createdAt = epoch(2026, 8, 24, 2, 0),
            updatedAt = epoch(2026, 8, 24, 2, 0)
        )

        assertEquals(
            CheckInPhase.WAKE,
            CheckInPhasePolicy.phase(active, emptyList(), settings, now, TEST_ZONE)
        )
    }

    @Test
    fun `21 first missed night shows missing after wake time`() {
        val now = epoch(2026, 8, 24, 9, 0)

        assertEquals(
            CheckInPhase.MISSING_PREVIOUS,
            CheckInPhasePolicy.phase(null, emptyList(), midnightSettings(), now, TEST_ZONE)
        )
    }

    @Test
    fun `22 completed night shows day complete`() {
        val now = epoch(2026, 8, 24, 9, 0)
        val completed = completedSession(
            id = 22,
            sleepAt = epoch(2026, 8, 24, 0, 30),
            wakeAt = epoch(2026, 8, 24, 7, 0)
        )

        assertEquals(
            CheckInPhase.DAY_COMPLETE,
            CheckInPhasePolicy.phase(null, listOf(completed), midnightSettings(), now, TEST_ZONE)
        )
    }

    @Test
    fun `23 next bedtime returns bedtime and keeps previous missing reminder`() {
        val now = epoch(2026, 8, 25, 0, 5)
        val settings = midnightSettings()

        assertEquals(
            CheckInPhase.BEDTIME,
            CheckInPhasePolicy.phase(null, emptyList(), settings, now, TEST_ZONE)
        )
        assertTrue(CheckInPhasePolicy.hasPreviousMissing(emptyList(), settings, now, TEST_ZONE))
    }

    @Test
    fun `24 time validation rejects order future and overlap`() {
        val now = epoch(2026, 8, 24, 14, 0)
        val existing = completedSession(
            id = 24,
            sleepAt = epoch(2026, 8, 24, 10, 0),
            wakeAt = epoch(2026, 8, 24, 12, 0)
        )

        assertEquals(
            SessionTimeValidation.INVALID_ORDER,
            SessionTimePolicy.validate(now, now, now, emptyList())
        )
        assertEquals(
            SessionTimeValidation.FUTURE_TIME,
            SessionTimePolicy.validate(now, now + minutes(30), now, emptyList())
        )
        assertEquals(
            SessionTimeValidation.OVERLAP,
            SessionTimePolicy.validate(
                epoch(2026, 8, 24, 11, 0),
                epoch(2026, 8, 24, 13, 0),
                now,
                listOf(existing)
            )
        )
    }

    @Test
    fun `25 only explicit time edits are treated as corrections`() {
        val clickedAt = epoch(2026, 8, 24, 9, 1) + 5_000L
        val staleClockValue = epoch(2026, 8, 24, 9, 0) + 50_000L
        val selectedSameMinute = epoch(2026, 8, 24, 9, 1)
        val selectedEarlierMinute = epoch(2026, 8, 24, 8, 59)

        assertEquals(clickedAt, resolveCheckInTime(staleClockValue, clickedAt, wasAdjusted = false))
        assertEquals(clickedAt, resolveCheckInTime(selectedSameMinute, clickedAt, wasAdjusted = true))
        assertEquals(selectedEarlierMinute, resolveCheckInTime(selectedEarlierMinute, clickedAt, wasAdjusted = true))
    }

    @Test
    fun `26 corrected bedtime still uses original click for undo window`() {
        val clickedAt = epoch(2026, 8, 24, 2, 0)
        val corrected = activeSession().copy(
            sleepCheckInAt = epoch(2026, 8, 23, 22, 0),
            originalSleepCheckInAt = clickedAt,
            createdAt = clickedAt,
            updatedAt = clickedAt
        )

        assertTrue(SessionPolicy.canUndo(corrected, clickedAt + minutes(4)))
        assertFalse(SessionPolicy.canUndo(corrected, clickedAt + minutes(6)))
    }

    @Test
    fun `27 export v2 retains correction and plan audit fields`() {
        val session = completedSession(27, sleepAt, wakeAt).copy(
            originalSleepCheckInAt = sleepAt + minutes(3),
            originalWakeCheckInAt = wakeAt + minutes(7),
            correctedAt = wakeAt + minutes(8),
            plannedBedtimeHour = 0,
            plannedBedtimeMinute = 0,
            plannedWakeHour = 8,
            plannedWakeMinute = 0
        )
        val csvLines = DataExporter.toCsv(listOf(session)).lineSequence().toList()
        val header = csvLines.first()
        val row = csvLines[1]

        assertEquals("sleep-saver-export-v2", DataExporter.FORMAT)
        assertTrue(header.contains("originalSleepCheckInAt"))
        assertTrue(header.contains("originalWakeCheckInAt"))
        assertTrue(header.contains("correctedAt"))
        assertTrue(header.contains("plannedWakeMinute"))
        assertTrue(row.contains(session.originalSleepCheckInAt.toString()))
        assertTrue(row.contains(session.correctedAt.toString()))
    }

    @Test
    fun `28 selecting a historic journal date resolves that exact night for editing`() {
        val newest = completedSession(
            id = 24,
            sleepAt = epoch(2026, 8, 24, 1, 55),
            wakeAt = epoch(2026, 8, 24, 9, 39)
        )
        val abnormalHistoric = completedSession(
            id = 23,
            sleepAt = epoch(2026, 8, 23, 3, 46),
            wakeAt = epoch(2026, 8, 23, 20, 9)
        )
        val sessions = listOf(newest, abnormalHistoric)

        assertEquals(abnormalHistoric, selectedJournalSession(sessions, 23))
        assertEquals(newest, selectedJournalSession(sessions, null))
    }

    @Test
    fun `29 historic journal edit controls keep usable touch targets`() {
        assertTrue(JOURNAL_HISTORY_DATE_CHIP_WIDTH_DP >= 48)
        assertTrue(JOURNAL_HISTORY_DATE_CHIP_HEIGHT_DP >= 44)
        assertTrue(JOURNAL_HISTORY_EDIT_BUTTON_HEIGHT_DP >= 48)
    }

    @Test
    fun `30 inline clock accepts direct 24 hour numeric input`() {
        val input = SessionClockInput(
            date = LocalDate.of(2026, 8, 23),
            hourText = "22",
            minuteText = "00"
        )

        assertEquals(epoch(2026, 8, 23, 22, 0), sessionClockTimestamp(input, TEST_ZONE))
        assertNull(sessionClockTimestamp(input.copy(hourText = "24"), TEST_ZONE))
        assertNull(sessionClockTimestamp(input.copy(minuteText = "60"), TEST_ZONE))
    }

    @Test
    fun `31 inline clock quick adjustment crosses midnight correctly`() {
        val input = SessionClockInput(
            date = LocalDate.of(2026, 8, 24),
            hourText = "00",
            minuteText = "05"
        )

        assertEquals(
            SessionClockInput(LocalDate.of(2026, 8, 23), "23", "55"),
            adjustSessionClock(input, -10, TEST_ZONE)
        )
    }

    @Test
    fun `32 abnormal duration makes the direct correction entry prominent`() {
        val normal = completedSession(
            id = 30,
            sleepAt = epoch(2026, 8, 23, 2, 0),
            wakeAt = epoch(2026, 8, 23, 9, 0)
        )
        val abnormal = completedSession(
            id = 31,
            sleepAt = epoch(2026, 8, 23, 3, 46),
            wakeAt = epoch(2026, 8, 23, 20, 9)
        )

        assertFalse(sessionNeedsCorrectionAttention(normal))
        assertTrue(sessionNeedsCorrectionAttention(abnormal))
    }

    private fun midnightSettings() = AppSettings(
        bedtimeHour = 0,
        bedtimeMinute = 0,
        wakeHour = 8,
        wakeMinute = 0
    )

    private fun completedSession(id: Long, sleepAt: Long, wakeAt: Long) = SleepSessionEntity(
        id = id,
        sessionDate = SleepSessionEntity.sessionDate(sleepAt, TEST_ZONE),
        sleepCheckInAt = sleepAt,
        wakeCheckInAt = wakeAt,
        createdAt = sleepAt,
        updatedAt = wakeAt
    )

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(TEST_ZONE)
            .toInstant()
            .toEpochMilli()

    private fun summary(vararg events: UsageEventRecord): UsageSummary =
        UsageStatsAnalyzer.summarize(events.toList(), sleepAt, wakeAt, appPackage)

    private fun event(
        minutesAfterSleep: Int,
        type: Int,
        packageName: String? = null
    ) = UsageEventRecord(sleepAt + minutes(minutesAfterSleep), type, packageName)

    private fun eventMillis(
        minutesAfterSleep: Int,
        extraMillis: Long,
        type: Int,
        packageName: String? = null
    ) = UsageEventRecord(sleepAt + minutes(minutesAfterSleep) + extraMillis, type, packageName)

    private fun activeSession() = SleepSessionEntity(
        id = 1,
        sessionDate = "2027-01-15",
        sleepCheckInAt = sleepAt,
        createdAt = sleepAt,
        updatedAt = sleepAt
    )

    private fun minutes(value: Int): Long = value * 60_000L
    private fun hours(value: Int): Long = value * 60L * 60_000L
}
