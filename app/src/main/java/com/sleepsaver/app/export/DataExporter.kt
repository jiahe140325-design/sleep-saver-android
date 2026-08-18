package com.sleepsaver.app.export

import com.sleepsaver.app.data.SleepSessionEntity
import org.json.JSONArray
import org.json.JSONObject

object DataExporter {
    fun toJson(sessions: List<SleepSessionEntity>): String {
        val rows = JSONArray()
        sessions.forEach { session ->
            rows.put(
                JSONObject()
                    .put("id", session.id)
                    .put("sessionDate", session.sessionDate)
                    .put("sleepCheckInAt", session.sleepCheckInAt)
                    .put("wakeCheckInAt", session.wakeCheckInAt ?: JSONObject.NULL)
                    .put("restWindowMinutes", session.restWindowMinutes ?: JSONObject.NULL)
                    .put("preSleepPhoneMinutes", session.preSleepPhoneMinutes ?: JSONObject.NULL)
                    .put("postCheckInPhoneMinutes", session.postCheckInPhoneMinutes ?: JSONObject.NULL)
                    .put("nightUnlockCount", session.nightUnlockCount ?: JSONObject.NULL)
                    .put("nightPhoneMinutes", session.nightPhoneMinutes ?: JSONObject.NULL)
                    .put("moodBeforeSleep", session.moodBeforeSleep ?: JSONObject.NULL)
                    .put("moodAfterWake", session.moodAfterWake ?: JSONObject.NULL)
                    .put("tags", JSONArray(session.tags()))
                    .put("isSupplemented", session.isSupplemented)
                    .put("usageDataAvailable", session.usageDataAvailable ?: JSONObject.NULL)
            )
        }
        return JSONObject()
            .put("format", "sleep-saver-export-v1")
            .put("exportedAt", System.currentTimeMillis())
            .put("sessions", rows)
            .toString(2)
    }

    fun toCsv(sessions: List<SleepSessionEntity>): String = buildString {
        appendLine(
            "sessionDate,sleepCheckInAt,wakeCheckInAt,restWindowMinutes," +
                "preSleepPhoneMinutes,postCheckInPhoneMinutes,nightUnlockCount," +
                "nightPhoneMinutes,moodBeforeSleep,moodAfterWake,tags,isSupplemented,usageDataAvailable"
        )
        sessions.forEach { session ->
            appendLine(
                listOf(
                    session.sessionDate,
                    session.sleepCheckInAt,
                    session.wakeCheckInAt,
                    session.restWindowMinutes,
                    session.preSleepPhoneMinutes,
                    session.postCheckInPhoneMinutes,
                    session.nightUnlockCount,
                    session.nightPhoneMinutes,
                    session.moodBeforeSleep,
                    session.moodAfterWake,
                    session.tags().joinToString("|"),
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

