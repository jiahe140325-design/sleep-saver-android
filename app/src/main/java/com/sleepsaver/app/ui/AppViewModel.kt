package com.sleepsaver.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sleepsaver.app.SleepSaverApplication
import com.sleepsaver.app.data.AppSettings
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

    fun startSleep(mood: String?, tags: List<String>) {
        viewModelScope.launch {
            busy.value = true
            when (sleepRepository.startSession(mood, tags)) {
                StartSessionResult.CREATED -> _messages.emit("睡前打卡完成，今晚开始记录")
                StartSessionResult.ALREADY_ACTIVE -> _messages.emit("今晚已经开始记录，请勿重复打卡")
            }
            busy.value = false
        }
    }

    fun finishWake(mood: String?) {
        val active = uiState.value.activeSession
        if (active == null) {
            _messages.tryEmit("请先完成睡前打卡")
            return
        }
        viewModelScope.launch {
            busy.value = true
            val now = System.currentTimeMillis()
            val usage = withContext(Dispatchers.IO) {
                usageAnalyzer.analyze(active.sleepCheckInAt, now)
            }
            val completed = sleepRepository.finishSession(mood, usage, now)
            _messages.emit(if (completed) "早起打卡完成，昨晚记录已保存" else "没有找到正在进行的记录")
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
}

