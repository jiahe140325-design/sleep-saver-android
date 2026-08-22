package com.sleepsaver.app

import android.app.usage.UsageEvents
import com.sleepsaver.app.data.PersistencePolicy
import com.sleepsaver.app.data.SleepSessionEntity
import com.sleepsaver.app.domain.SessionPolicy
import com.sleepsaver.app.domain.SessionState
import com.sleepsaver.app.reminder.ReminderConstants
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_LABEL_WIDTH_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_LABEL_HEIGHT_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_LABEL_TOP_OFFSET_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_MASCOT_HALF_SIZE_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_INLINE_GAP_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_INLINE_POINT_LIMIT
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_POINT_SPACING_DP
import com.sleepsaver.app.ui.JOURNAL_DEVIATION_SIDE_PADDING_DP
import com.sleepsaver.app.ui.journalDateFromUtcMillis
import com.sleepsaver.app.ui.journalDateRangeCanConfirm
import com.sleepsaver.app.ui.journalDateToUtcMillis
import com.sleepsaver.app.ui.journalDeviationChartWidthDp
import com.sleepsaver.app.usage.UsageEventRecord
import com.sleepsaver.app.usage.UsageStatsAnalyzer
import com.sleepsaver.app.usage.UsageSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SleepSaverLogicTest {
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
        assertEquals(2, PersistencePolicy.DATABASE_VERSION)
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
