package com.sleepsaver.app.export

import com.sleepsaver.app.data.SleepSessionEntity
import org.json.JSONArray
import org.json.JSONObject

object DataExporter {
    const val FORMAT = "sleep-saver-export-v2"

    fun toJson(sessions: List<SleepSessionEntity>): String {
        val rows = JSONArray()
        sessions.forEach { session ->
            rows.put(
                JSONObject()
                    .put("id", session.id)
                    .put("sessionDate", session.sessionDate)
                    .put("sleepCheckInAt", session.sleepCheckInAt)
                    .put("wakeCheckInAt", session.wakeCheckInAt ?: JSONObject.NULL)
                    .put("originalSleepCheckInAt", session.originalSleepCheckInAt ?: JSONObject.NULL)
                    .put("originalWakeCheckInAt", session.originalWakeCheckInAt ?: JSONObject.NULL)
                    .put("correctedAt", session.correctedAt ?: JSONObject.NULL)
                    .put("plannedBedtimeHour", session.plannedBedtimeHour ?: JSONObject.NULL)
                    .put("plannedBedtimeMinute", session.plannedBedtimeMinute ?: JSONObject.NULL)
                    .put("plannedWakeHour", session.plannedWakeHour ?: JSONObject.NULL)
                    .put("plannedWakeMinute", session.plannedWakeMinute ?: JSONObject.NULL)
                    .put("restWindowMinutes", session.restWindowMinutes ?: JSONObject.NULL)
                    .put("preSleepPhoneMinutes", session.preSleepPhoneMinutes ?: JSONObject.NULL)
                    .put("postCheckInPhoneMinutes", session.postCheckInPhoneMinutes ?: JSONObject.NULL)
                    .put("nightUnlockCount", session.nightUnlockCount ?: JSONObject.NULL)
                    .put("nightPhoneMinutes", session.nightPhoneMinutes ?: JSONObject.NULL)
                    .put("moodBeforeSleep", session.moodBeforeSleep ?: JSONObject.NULL)
                    .put("moodAfterWake", session.moodAfterWake ?: JSONObject.NULL)
                    .put("tags", JSONArray(session.tags()))
                    .put("isSleepCheckInManual", session.isSleepCheckInManual)
                    .put("isWakeCheckInManual", session.isWakeCheckInManual)
                    .put("isSupplemented", session.isSupplemented)
                    .put("usageDataAvailable", session.usageDataAvailable ?: JSONObject.NULL)
            )
        }
        return JSONObject()
            .put("format", FORMAT)
            .put("exportedAt", System.currentTimeMillis())
            .put("sessions", rows)
            .toString(2)
    }

    fun toCsv(sessions: List<SleepSessionEntity>): String = buildString {
        appendLine(
            "id,sessionDate,sleepCheckInAt,wakeCheckInAt,originalSleepCheckInAt," +
                "originalWakeCheckInAt,correctedAt,plannedBedtimeHour,plannedBedtimeMinute," +
                "plannedWakeHour,plannedWakeMinute,restWindowMinutes,preSleepPhoneMinutes," +
                "postCheckInPhoneMinutes,nightUnlockCount,nightPhoneMinutes,moodBeforeSleep," +
                "moodAfterWake,tags,isSleepCheckInManual,isWakeCheckInManual,isSupplemented," +
                "usageDataAvailable"
        )
        sessions.forEach { session ->
            appendLine(
                listOf(
                    session.id,
                    session.sessionDate,
                    session.sleepCheckInAt,
                    session.wakeCheckInAt,
                    session.originalSleepCheckInAt,
                    session.originalWakeCheckInAt,
                    session.correctedAt,
                    session.plannedBedtimeHour,
                    session.plannedBedtimeMinute,
                    session.plannedWakeHour,
                    session.plannedWakeMinute,
                    session.restWindowMinutes,
                    session.preSleepPhoneMinutes,
                    session.postCheckInPhoneMinutes,
                    session.nightUnlockCount,
                    session.nightPhoneMinutes,
                    session.moodBeforeSleep,
                    session.moodAfterWake,
                    session.tags().joinToString("|"),
                    session.isSleepCheckInManual,
                    session.isWakeCheckInManual,
                    session.isSupplemented,
                    session.usageDataAvailable
                ).joinToString(",") { csvCell(it) }
            )
        }
    }

    private fun csvCell(value: Any?): String {
        val raw = value?.toString().orEmpty()
        return "\"${raw.replace("\"", "\"\"")}\""
    }
}

