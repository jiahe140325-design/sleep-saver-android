package com.sleepsaver.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsaver.app.SleepSaverApplication
import com.sleepsaver.app.data.AppSettings
import com.sleepsaver.app.data.SessionEditResult
import com.sleepsaver.app.data.SleepSessionEntity
import com.sleepsaver.app.data.StartSessionResult
import com.sleepsaver.app.export.DataExporter
import com.sleepsaver.app.reminder.SleepReminderScheduler
import com.sleepsaver.app.usage.UsageAccess
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppUiState(
    val activeSession: SleepSessionEntity? = null,
    val sessions: List<SleepSessionEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val usagePermissionGranted: Boolean = false,
    val busy: Boolean = false
) {
    val completedSessions: List<SleepSessionEntity>
        get() = sessions.filter { !it.isActive }

    val latestCompleted: SleepSessionEntity?
        get() = completedSessions.firstOrNull()

    val weekRestMinutes: Long
        get() {
            val earliestDate = LocalDate.now().minusDays(6)
            return completedSessions
                .filter { runCatching { LocalDate.parse(it.sessionDate) >= earliestDate }.getOrDefault(false) }
                .sumOf { it.restWindowMinutes ?: 0L }
        }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SleepSaverApplication
    private val sleepRepository = app.container.sleepRepository
    private val settingsRepository = app.container.settingsRepository
    private val usageAnalyzer = app.container.usageStatsAnalyzer

    private val usageGranted = MutableStateFlow(false)
    private val busy = MutableStateFlow(false)
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val uiState: StateFlow<AppUiState> = combine(
        sleepRepository.activeSession,
        sleepRepository.sessions,
        settingsRepository.settings,
        usageGranted,
        busy
    ) { active, sessions, settings, permission, isBusy ->
        AppUiState(active, sessions, settings, permission, isBusy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState()
    )

    init {
        refreshUsagePermission()
    }

    fun refreshUsagePermission() {
        usageGranted.value = UsageAccess.isGranted(getApplication())
    }

    fun startSleep(
        mood: String?,
        tags: List<String>,
        effectiveSleepAt: Long,
        sleepWasAdjusted: Boolean
    ) {
        val settings = uiState.value.settings
        val recordedAt = System.currentTimeMillis()
        viewModelScope.launch {
            busy.value = true
            when (
                sleepRepository.startSession(
                    moodBeforeSleep = mood,
                    tags = tags,
                    settings = settings,
                    effectiveSleepAt = effectiveSleepAt,
                    timeWasAdjusted = sleepWasAdjusted,
                    recordedAt = recordedAt
                )
            ) {
                StartSessionResult.CREATED -> _messages.emit("睡前打卡完成，今晚开始记录")
                StartSessionResult.ALREADY_ACTIVE -> _messages.emit("今晚已经开始记录，请勿重复打卡")
                StartSessionResult.INVALID_TIME -> _messages.emit("记录时间不能晚于当前时间")
            }
            busy.value = false
        }
    }

    fun finishWake(
        mood: String?,
        effectiveSleepAt: Long,
        effectiveWakeAt: Long,
        sleepWasAdjusted: Boolean,
        wakeWasAdjusted: Boolean
    ) {
        val active = uiState.value.activeSession
        if (active == null) {
            _messages.tryEmit("请先完成睡前打卡")
            return
        }
        val recordedAt = System.currentTimeMillis()
        if (!timeRangeLooksValid(effectiveSleepAt, effectiveWakeAt, recordedAt)) return
        viewModelScope.launch {
            busy.value = true
            val usage = analyzeUsage(effectiveSleepAt, effectiveWakeAt)
            val result = sleepRepository.finishSession(
                moodAfterWake = mood,
                usageSummary = usage,
                effectiveSleepAt = effectiveSleepAt,
                effectiveWakeAt = effectiveWakeAt,
                sleepWasAdjusted = sleepWasAdjusted,
                wakeWasAdjusted = wakeWasAdjusted,
                recordedAt = recordedAt
            )
            emitEditResult(result, "早起打卡完成，昨晚记录已保存")
            busy.value = false
        }
    }

    fun supplementSession(sleepAt: Long, wakeAt: Long) {
        val recordedAt = System.currentTimeMillis()
        if (!timeRangeLooksValid(sleepAt, wakeAt, recordedAt)) return
        val settings = uiState.value.settings
        viewModelScope.launch {
            busy.value = true
            val result = sleepRepository.supplementSession(
                sleepAt = sleepAt,
                wakeAt = wakeAt,
                settings = settings,
                usageSummary = analyzeUsage(sleepAt, wakeAt),
                recordedAt = recordedAt
            )
            emitEditResult(result, "昨晚已经补记，统计和趋势已重新计算")
            busy.value = false
        }
    }

    fun correctSession(id: Long, sleepAt: Long, wakeAt: Long) {
        val correctedAt = System.currentTimeMillis()
        if (!timeRangeLooksValid(sleepAt, wakeAt, correctedAt)) return
        viewModelScope.launch {
            busy.value = true
            val result = sleepRepository.correctSession(
                id = id,
                sleepAt = sleepAt,
                wakeAt = wakeAt,
                usageSummary = analyzeUsage(sleepAt, wakeAt),
                correctedAt = correctedAt
            )
            emitEditResult(result, "时间已调整，统计和趋势已重新计算")
            busy.value = false
        }
    }

    fun restoreOriginalSession(id: Long) {
        val session = uiState.value.sessions.firstOrNull { it.id == id }
        if (session == null) {
            _messages.tryEmit("没有找到这条记录")
            return
        }
        val sleepAt = session.originalSleepCheckInAt ?: session.sleepCheckInAt
        val wakeAt = session.originalWakeCheckInAt ?: session.wakeCheckInAt
        if (wakeAt == null) {
            _messages.tryEmit("这条记录还没有完成")
            return
        }
        viewModelScope.launch {
            busy.value = true
            val result = sleepRepository.restoreOriginalSession(
                id = id,
                usageSummary = analyzeUsage(sleepAt, wakeAt)
            )
            emitEditResult(result, "已恢复最初打卡时间")
            busy.value = false
        }
    }

    fun undoSleepCheckIn() {
        viewModelScope.launch {
            val undone = sleepRepository.undoActiveSession()
            _messages.emit(if (undone) "已撤销本次睡前打卡" else "只能在打卡后 5 分钟内撤销")
        }
    }

    fun setBedtime(hour: Int, minute: Int) {
        viewModelScope.launch {
            settingsRepository.setBedtime(hour, minute)
            val next = uiState.value.settings.copy(bedtimeHour = hour, bedtimeMinute = minute)
            SleepReminderScheduler.apply(getApplication(), next)
        }
    }

    fun setWakeTime(hour: Int, minute: Int) {
        viewModelScope.launch { settingsRepository.setWakeTime(hour, minute) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReminderEnabled(enabled)
            SleepReminderScheduler.apply(
                getApplication(),
                uiState.value.settings.copy(reminderEnabled = enabled)
            )
            _messages.emit(if (enabled) "每晚睡眠提醒已开启" else "睡眠提醒已关闭")
        }
    }

    suspend fun jsonExport(): String = withContext(Dispatchers.IO) {
        DataExporter.toJson(sleepRepository.allSessionsOnce())
    }

    suspend fun csvExport(): String = withContext(Dispatchers.IO) {
        DataExporter.toCsv(sleepRepository.allSessionsOnce())
    }

    private suspend fun analyzeUsage(sleepAt: Long, wakeAt: Long) = withContext(Dispatchers.IO) {
        usageAnalyzer.analyze(sleepAt, wakeAt)
    }

    private fun timeRangeLooksValid(sleepAt: Long, wakeAt: Long, now: Long): Boolean {
        val message = when {
            wakeAt <= sleepAt -> "起床时间必须晚于睡前时间"
            sleepAt > now || wakeAt > now -> "不能选择未来时间"
            else -> null
        }
        message?.let(_messages::tryEmit)
        return message == null
    }

    private suspend fun emitEditResult(result: SessionEditResult, successMessage: String) {
        _messages.emit(
            when (result) {
                SessionEditResult.SAVED -> successMessage
                SessionEditResult.NOT_FOUND -> "没有找到需要修改的记录"
                SessionEditResult.INVALID_ORDER -> "起床时间必须晚于睡前时间"
                SessionEditResult.FUTURE_TIME -> "不能选择未来时间"
                SessionEditResult.OVERLAP -> "这个时间段与已有睡眠记录重叠"
            }
        )
    }
}
