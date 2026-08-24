package com.sleepsaver.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity(
    tableName = "sleep_sessions",
    indices = [Index("sessionDate"), Index("wakeCheckInAt")]
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionDate: String,
    val sleepCheckInAt: Long,
    val wakeCheckInAt: Long? = null,
    val plannedBedtimeHour: Int? = null,
    val plannedBedtimeMinute: Int? = null,
    val plannedWakeHour: Int? = null,
    val plannedWakeMinute: Int? = null,
    val originalSleepCheckInAt: Long? = null,
    val originalWakeCheckInAt: Long? = null,
    val correctedAt: Long? = null,
    val preSleepPhoneMinutes: Int? = null,
    val postCheckInPhoneMinutes: Int? = null,
    val nightUnlockCount: Int? = null,
    val nightPhoneMinutes: Int? = null,
    val moodBeforeSleep: String? = null,
    val moodAfterWake: String? = null,
    val tagsJson: String = "[]",
    val isSleepCheckInManual: Boolean = true,
    val isWakeCheckInManual: Boolean = true,
    val isSupplemented: Boolean = false,
    val usageDataAvailable: Boolean? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isActive: Boolean get() = wakeCheckInAt == null
    val wasCorrected: Boolean
        get() = originalSleepCheckInAt != null || originalWakeCheckInAt != null
    val restWindowMinutes: Long?
        get() = wakeCheckInAt?.let { ((it - sleepCheckInAt).coerceAtLeast(0L)) / 60_000L }

    fun tags(): List<String> = runCatching {
        val array = JSONArray(tagsJson)
        List(array.length()) { index -> array.getString(index) }
    }.getOrDefault(emptyList())

    companion object {
        fun sessionDate(timestamp: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
            Instant.ofEpochMilli(timestamp)
                .atZone(zoneId)
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

enum class StartSessionResult {
    CREATED,
    ALREADY_ACTIVE,
    INVALID_TIME
}

enum class SessionEditResult {
    SAVED,
    NOT_FOUND,
    INVALID_ORDER,
    FUTURE_TIME,
    OVERLAP
}
