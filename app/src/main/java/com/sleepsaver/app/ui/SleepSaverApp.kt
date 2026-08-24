package com.sleepsaver.app.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.SentimentNeutral
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material.icons.rounded.SentimentVerySatisfied
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepsaver.app.R
import com.sleepsaver.app.data.AppSettings
import com.sleepsaver.app.data.SleepSessionEntity
import com.sleepsaver.app.domain.CheckInPhase
import com.sleepsaver.app.domain.CheckInPhasePolicy
import com.sleepsaver.app.domain.PlannedSleepWindow
import com.sleepsaver.app.domain.SessionPolicy
import com.sleepsaver.app.ui.theme.Blush
import com.sleepsaver.app.ui.theme.Cream
import com.sleepsaver.app.ui.theme.Ink
import com.sleepsaver.app.ui.theme.Lavender
import com.sleepsaver.app.ui.theme.Paper
import com.sleepsaver.app.ui.theme.Sage
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class AppTab(val title: String, val icon: ImageVector) {
    TODAY("今日", Icons.Rounded.Home),
    CHECK_IN("打卡", Icons.Rounded.NightsStay),
    JOURNAL("手帐", Icons.Rounded.MenuBook),
    FRIENDS("好友", Icons.Rounded.People)
}

private val cardShape = RoundedCornerShape(22.dp)

private data class CheckInOption(val label: String, val icon: ImageVector)

private val moodOptions = listOf(
    CheckInOption("困得不行", Icons.Rounded.SentimentVeryDissatisfied),
    CheckInOption("还可以", Icons.Rounded.SentimentNeutral),
    CheckInOption("挺精神", Icons.Rounded.SentimentVerySatisfied)
)

private val bedtimeTagOptions = listOf(
    CheckInOption("加班了", Icons.Rounded.Work),
    CheckInOption("刷手机", Icons.Rounded.PhoneAndroid),
    CheckInOption("想事情", Icons.Rounded.Psychology),
    CheckInOption("喝了咖啡", Icons.Rounded.Coffee),
    CheckInOption("运动了", Icons.Rounded.DirectionsRun),
    CheckInOption("今天很平静", Icons.Rounded.Spa)
)

private data class JournalWeekPoint(
    val periodStart: LocalDate,
    val averageSleepMinutes: Int?,
    val averageDeviationMinutes: Int?
)

private enum class JournalRangePreset(val label: String) {
    RECENT_7("近7晚"),
    RECENT_30("近30晚"),
    CUSTOM("自定义")
}

private enum class JournalGrouping(val label: String) {
    DAY("按天"),
    WEEK("按周"),
    MONTH("按月")
}

private data class JournalDateRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
    val label: String
) {
    companion object {
        fun recentDays(days: Long, label: String): JournalDateRange {
            val end = LocalDate.now()
            return JournalDateRange(end.minusDays(days - 1), end, label)
        }
    }
}

internal data class SessionClockInput(
    val date: LocalDate,
    val hourText: String,
    val minuteText: String
)

internal fun sessionClockInput(
    timestamp: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): SessionClockInput {
    val value = Instant.ofEpochMilli(timestamp).atZone(zoneId)
    return SessionClockInput(
        date = value.toLocalDate(),
        hourText = value.hour.toString().padStart(2, '0'),
        minuteText = value.minute.toString().padStart(2, '0')
    )
}

internal fun sessionClockTimestamp(
    input: SessionClockInput,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long? {
    val hour = input.hourText.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val minute = input.minuteText.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return input.date
        .atTime(hour, minute)
        .atZone(zoneId)
        .toInstant()
        .toEpochMilli()
}

internal fun adjustSessionClock(
    input: SessionClockInput,
    minutes: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): SessionClockInput {
    val timestamp = sessionClockTimestamp(input, zoneId) ?: return input
    val adjusted = Instant.ofEpochMilli(timestamp).atZone(zoneId).plusMinutes(minutes)
    return SessionClockInput(
        date = adjusted.toLocalDate(),
        hourText = adjusted.hour.toString().padStart(2, '0'),
        minuteText = adjusted.minute.toString().padStart(2, '0')
    )
}

internal fun sessionNeedsCorrectionAttention(session: SleepSessionEntity): Boolean =
    session.restWindowMinutes?.let { it !in 60L..(16L * 60L) } == true

private val journalDurationTapeResources = listOf(
    R.drawable.journal_duration_tape_1,
    R.drawable.journal_duration_tape_2,
    R.drawable.journal_duration_tape_3,
    R.drawable.journal_duration_tape_4,
    R.drawable.journal_duration_tape_5,
    R.drawable.journal_duration_tape_6,
    R.drawable.journal_duration_tape_7
)

@Composable
fun SleepSaverApp(viewModel: AppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(AppTab.TODAY) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                AppTab.TODAY -> TodayScreen(state, viewModel, snackbar)
                AppTab.CHECK_IN -> CheckInScreen(state, viewModel)
                AppTab.JOURNAL -> JournalScreen(state.completedSessions, state.settings, viewModel, snackbar)
                AppTab.FRIENDS -> FriendsUnavailableScreen()
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodayScreen(
    state: AppUiState,
    viewModel: AppViewModel,
    snackbar: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var enableReminderAfterPermission by remember { mutableStateOf(false) }
    var editingLatestId by remember { mutableStateOf<Long?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && enableReminderAfterPermission) viewModel.setReminderEnabled(true)
        else if (!granted) scope.launch { snackbar.showSnackbar("未获得通知权限，提醒没有开启") }
        enableReminderAfterPermission = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { TodayJournalHeader() }
        if (!state.usagePermissionGranted) {
            item { UsagePermissionCard { openUsageAccessSettings(context) } }
        }
        item { TodayTimelineCard(state.activeSession, state.latestCompleted) }
        if (state.activeSession == null) {
            state.latestCompleted?.let { latest ->
                item {
                    TodayCorrectionEntry(latest) {
                        editingLatestId = latest.id
                    }
                }
            }
        }
        item { CompactWeekProgressTicket(state) }
        item { TodayBlushMemo() }
        item {
            CompactSleepScheduleCard(
                settings = state.settings,
                onBedtime = viewModel::setBedtime,
                onWakeTime = viewModel::setWakeTime,
                onReminderChange = { enabled ->
                    if (!enabled) {
                        viewModel.setReminderEnabled(false)
                    } else if (Build.VERSION.SDK_INT >= 33 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        enableReminderAfterPermission = true
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setReminderEnabled(true)
                    }
                }
            )
        }
    }

    state.completedSessions.firstOrNull { it.id == editingLatestId }?.let { session ->
        session.wakeCheckInAt?.let { wakeAt ->
            SessionTimeEditorDialog(
                title = "纠正昨晚记录",
                description = "直接改数字，不再连续打开日期盘和钟表盘。保存后会立即重算今日与手帐统计。",
                initialSleepAt = session.sleepCheckInAt,
                initialWakeAt = wakeAt,
                onDismiss = { editingLatestId = null },
                onConfirm = { selectedSleepAt, selectedWakeAt ->
                    viewModel.correctSession(session.id, selectedSleepAt, selectedWakeAt)
                    editingLatestId = null
                },
                onRestore = if (session.wasCorrected) {
                    {
                        viewModel.restoreOriginalSession(session.id)
                        editingLatestId = null
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun TodayJournalHeader() {
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINA))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("今天过得怎么样", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(date, fontSize = 13.sp, color = Ink.copy(alpha = .68f))
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Lavender.copy(alpha = .32f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = Ink, modifier = Modifier.size(27.dp))
        }
    }
}

@Composable
private fun UsagePermissionCard(onOpenSettings: () -> Unit) {
    Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = .42f))) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Ink)
                Spacer(Modifier.width(8.dp))
                Text("需要使用情况访问权限", fontWeight = FontWeight.Bold)
            }
            Text("只用于统计是否真正解锁、夜间使用时段和睡前手机时长。原始 App 轨迹不会保存，也不会上传。")
            Button(onClick = onOpenSettings) { Text("去授权") }
        }
    }
}

@Composable
private fun TodayTimelineCard(active: SleepSessionEntity?, latest: SleepSessionEntity?) {
    val session = active ?: latest
    val durationMinutes = when {
        active != null -> ((System.currentTimeMillis() - active.sleepCheckInAt).coerceAtLeast(0L)) / 60_000L
        latest != null -> latest.restWindowMinutes
        else -> null
    }
    val hours = durationMinutes?.div(60)
    val minutes = durationMinutes?.rem(60)
    val wakeValue = when {
        active != null -> "待打卡"
        latest?.wakeCheckInAt != null -> formatTime(latest.wakeCheckInAt)
        else -> "—"
    }
    val unlockValue = when {
        active != null -> "待结算"
        latest == null -> "—"
        latest.usageDataAvailable == true -> latest.nightUnlockCount?.let { "$it 次" } ?: "0 次"
        latest.usageDataAvailable == false -> "不可用"
        else -> "—"
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F1FF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(304.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = 21.dp, y = 37.dp)
                    .width(3.dp)
                    .height(216.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Lavender)
            )

            TimelineIcon(
                icon = Icons.Rounded.NightsStay,
                tint = Ink,
                modifier = Modifier.offset(x = 0.dp, y = 0.dp)
            )
            Column(
                modifier = Modifier.offset(x = 60.dp, y = 0.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(if (active != null) "今晚休息" else "昨晚休息", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("睡前打卡", color = Ink.copy(alpha = .72f), fontSize = 13.sp)
                Text(session?.let { formatTime(it.sleepCheckInAt) } ?: "还没有记录", color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }

            Box(
                modifier = Modifier
                    .offset(x = 88.dp, y = 64.dp)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Paper)
                    .border(BorderStroke(3.dp, Lavender), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (durationMinutes == null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("等你来", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("打卡", color = Ink.copy(alpha = .7f), fontSize = 15.sp)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$hours", color = Ink, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                            Text(" 小时", color = Ink, fontSize = 14.sp, modifier = Modifier.padding(bottom = 7.dp))
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$minutes", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Text(" 分", color = Ink, fontSize = 13.sp, modifier = Modifier.padding(bottom = 5.dp))
                        }
                        if (active != null) Text("记录中", color = Sage, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Image(
                painter = painterResource(R.drawable.today_timeline_mascot),
                contentDescription = "蓝色睡帽小玩偶",
                modifier = Modifier
                    .offset(x = 182.dp, y = 54.dp)
                    .size(82.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = 182.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TimelineEventRow(
                    icon = Icons.Rounded.WbSunny,
                    tint = Color(0xFFE4A31A),
                    label = "早起打卡",
                    value = wakeValue
                )
                TimelineEventRow(
                    icon = Icons.Rounded.Lock,
                    tint = Ink,
                    label = "夜间解锁",
                    value = unlockValue
                )
            }
        }
    }
}

@Composable
private fun TodayCorrectionEntry(session: SleepSessionEntity, onClick: () -> Unit) {
    val needsAttention = sessionNeedsCorrectionAttention(session)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (needsAttention) Blush.copy(alpha = .46f) else Paper
        ),
        border = BorderStroke(
            1.dp,
            if (needsAttention) Blush else Lavender.copy(alpha = .68f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = Ink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (needsAttention) "这条记录可能有误" else "昨晚时间不对？",
                    color = Ink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("睡前、起床时间都可以纠正", color = Ink.copy(alpha = .62f), fontSize = 10.sp)
            }
            Text(if (needsAttention) "立即纠正" else "纠正记录", color = Lavender, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimelineEventRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimelineIcon(icon = icon, tint = tint)
        Spacer(Modifier.width(18.dp))
        TimelineValue(label = label, value = value)
    }
}

@Composable
private fun TimelineIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Paper)
            .border(BorderStroke(2.dp, Lavender), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun TimelineValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(label, color = Ink.copy(alpha = .72f), fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp, maxLines = 1)
    }
}

@Composable
private fun CompactWeekProgressTicket(state: AppUiState) {
    val target = state.settings.targetSleepMinutes * 7L
    val progress = if (target == 0L) 0f else (state.weekRestMinutes.toFloat() / target).coerceIn(0f, 1f)
    Card(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color(0xFFE7D7B4)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9EA))
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Flag, contentDescription = null, tint = Sage, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("本周目标", fontWeight = FontWeight.Bold, color = Ink)
                }
                Text("${state.weekRestMinutes / 60}/${target / 60} 小时", color = Ink, fontWeight = FontWeight.SemiBold)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = Sage,
                trackColor = Color(0xFFE8DFCF)
            )
            Text("记录的是两次打卡之间的休息窗口", color = Ink.copy(alpha = .72f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun TodayBlushMemo() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE1E3))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("今晚放过自己", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("睡眠不是任务，是给白天的自己充电。", color = Ink.copy(alpha = .74f), fontSize = 12.sp)
            }
            Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = Color(0xFFE77A98), modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun GentleNoteCard() {
    Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = Blush.copy(alpha = .7f))) {
        Column(Modifier.padding(20.dp)) {
            Text("今晚放过自己", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("睡眠不是任务，是给白天的自己充电。")
        }
    }
}

@Composable
private fun CompactSleepScheduleCard(
    settings: AppSettings,
    onBedtime: (Int, Int) -> Unit,
    onWakeTime: (Int, Int) -> Unit,
    onReminderChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE3D8C7)),
        colors = CardDefaults.cardColors(containerColor = Paper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Lavender.copy(alpha = .34f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = Ink)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("睡眠定时", color = Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                TimeButton(
                    modifier = Modifier,
                    label = "就寝",
                    hour = settings.bedtimeHour,
                    minute = settings.bedtimeMinute
                ) {
                    showTimePicker(context, settings.bedtimeHour, settings.bedtimeMinute, onBedtime)
                }
                Text("→", color = Lavender, fontWeight = FontWeight.Bold)
                TimeButton(
                    modifier = Modifier,
                    label = "起床",
                    hour = settings.wakeHour,
                    minute = settings.wakeMinute
                ) {
                    showTimePicker(context, settings.wakeHour, settings.wakeMinute, onWakeTime)
                }
                }
                Text("每晚重复 · 提前 ${settings.reminderAdvanceMinutes} 分钟提醒", color = Ink.copy(alpha = .66f), fontSize = 10.sp)
            }
            Switch(checked = settings.reminderEnabled, onCheckedChange = onReminderChange)
        }
    }
}

@Composable
private fun TimeButton(
    modifier: Modifier,
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
        border = BorderStroke(1.dp, Lavender.copy(alpha = .7f))
    ) {
        Text("%02d:%02d".format(hour, minute), color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun ExportCard(onJson: () -> Unit, onCsv: () -> Unit) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = .28f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("数据管理", fontWeight = FontWeight.Bold)
            }
            Text("需要备份或自己分析时再导出。保存位置由你选择，应用不会自行读取其他文件。", fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onJson) { Text("导出 JSON") }
                OutlinedButton(onClick = onCsv) { Text("导出 CSV") }
            }
        }
    }
}

@Composable
private fun CheckInScreen(state: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    val now = rememberMinuteClock()
    val phase = CheckInPhasePolicy.phase(
        activeSession = state.activeSession,
        completedSessions = state.completedSessions,
        settings = state.settings,
        now = now
    )
    val plannedWindow = CheckInPhasePolicy.windowAt(now, state.settings)
    val previousWindow = CheckInPhasePolicy.previousWindowAt(now, state.settings)
    val previousMissing = phase == CheckInPhase.BEDTIME &&
        CheckInPhasePolicy.hasPreviousMissing(state.completedSessions, state.settings, now)

    var mood by remember(phase, state.activeSession?.id) { mutableStateOf<String?>(null) }
    var selectedTags by remember(phase, state.activeSession?.id) { mutableStateOf(setOf<String>()) }
    var sleepAt by remember(phase, state.activeSession?.id, plannedWindow.bedtimeAt) {
        mutableLongStateOf(
            when (phase) {
                CheckInPhase.WAKE -> state.activeSession?.sleepCheckInAt ?: now
                CheckInPhase.MISSING_PREVIOUS -> plannedWindow.bedtimeAt
                else -> now
            }
        )
    }
    var wakeAt by remember(phase, state.activeSession?.id, plannedWindow.wakeAt) {
        mutableLongStateOf(
            when (phase) {
                CheckInPhase.MISSING_PREVIOUS -> plannedWindow.wakeAt.coerceAtMost(now)
                else -> now
            }
        )
    }
    var sleepAdjusted by remember(phase, state.activeSession?.id) { mutableStateOf(false) }
    var wakeAdjusted by remember(phase, state.activeSession?.id) { mutableStateOf(false) }
    var supplementWindow by remember { mutableStateOf<PlannedSleepWindow?>(null) }
    var editingCompletedId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(now, phase, sleepAdjusted, wakeAdjusted) {
        if (phase == CheckInPhase.BEDTIME && !sleepAdjusted) sleepAt = now
        if (phase == CheckInPhase.WAKE && !wakeAdjusted) wakeAt = now
    }

    val title = when (phase) {
        CheckInPhase.BEDTIME -> "睡前打卡"
        CheckInPhase.WAKE -> "早起打卡"
        CheckInPhase.MISSING_PREVIOUS -> "昨晚漏记"
        CheckInPhase.DAY_COMPLETE -> "昨晚已记录"
    }
    val subtitle = when (phase) {
        CheckInPhase.BEDTIME -> "告诉自己：今天到这里就好"
        CheckInPhase.WAKE -> "慢慢醒来，记录真实的感觉"
        CheckInPhase.MISSING_PREVIOUS -> "补上真实时间，这一页就完整了"
        CheckInPhase.DAY_COMPLETE -> "今天的数据已经收好，今晚再见"
    }
    val heroNote = when (phase) {
        CheckInPhase.BEDTIME -> "把今天的小事\n都放心放下吧"
        CheckInPhase.WAKE -> "新的一天醒来了\n先听听身体的感觉"
        CheckInPhase.MISSING_PREVIOUS -> "昨晚好像漏记了一页\n想起来时再补上就好"
        CheckInPhase.DAY_COMPLETE -> "昨晚已经好好记下\n今天不用重复打卡"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenTitle(title, subtitle) }
        item { CheckInJournalHero(note = heroNote) }
        if (!state.usagePermissionGranted && phase != CheckInPhase.DAY_COMPLETE) {
            item { UsagePermissionCard { openUsageAccessSettings(context) } }
        }
        when (phase) {
            CheckInPhase.BEDTIME -> {
                if (previousMissing) {
                    item {
                        PreviousMissingCard {
                            supplementWindow = previousWindow
                        }
                    }
                }
                item {
                    CheckInJournalSheet(
                        mood = mood,
                        onMoodSelected = { mood = it },
                        selectedTags = selectedTags,
                        onTagSelected = { label ->
                            selectedTags = if (label in selectedTags) selectedTags - label else selectedTags + label
                        }
                    )
                }
                item {
                    CheckInTimeAdjustCard(
                        sleepAt = sleepAt,
                        wakeAt = null,
                        onSleepChange = {
                            showDateTimePicker(context, sleepAt, now) {
                                sleepAt = it
                                sleepAdjusted = true
                            }
                        }
                    )
                }
                item {
                    PrimaryCheckInButton(
                        text = "我要睡了 · 开始记录",
                        icon = Icons.Rounded.NightsStay,
                        enabled = !state.busy,
                        onClick = {
                            viewModel.startSleep(mood, selectedTags.toList(), sleepAt, sleepAdjusted)
                        }
                    )
                }
            }

            CheckInPhase.WAKE -> {
                state.activeSession?.let { session ->
                    item { ActiveSessionCard(session) }
                    item {
                        CheckInTimeAdjustCard(
                            sleepAt = sleepAt,
                            wakeAt = wakeAt,
                            onSleepChange = {
                                showDateTimePicker(context, sleepAt, now) {
                                    sleepAt = it
                                    sleepAdjusted = true
                                }
                            },
                            onWakeChange = {
                                showDateTimePicker(context, wakeAt, now) {
                                    wakeAt = it
                                    wakeAdjusted = true
                                }
                            }
                        )
                    }
                    item {
                        MoodChoiceCard(
                            title = "醒来感觉怎么样？",
                            mood = mood,
                            onMoodSelected = { mood = it }
                        )
                    }
                    item {
                        PrimaryCheckInButton(
                            text = "完成早起打卡",
                            icon = Icons.Rounded.CheckCircle,
                            enabled = !state.busy,
                            onClick = {
                                viewModel.finishWake(
                                    mood,
                                    sleepAt,
                                    wakeAt,
                                    sleepAdjusted,
                                    wakeAdjusted
                                )
                            }
                        )
                    }
                    if (SessionPolicy.canUndo(session, now)) {
                        item {
                            OutlinedButton(
                                onClick = viewModel::undoSleepCheckIn,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("手滑了，撤销睡前打卡（5 分钟内）")
                            }
                        }
                    }
                }
            }

            CheckInPhase.MISSING_PREVIOUS -> {
                item {
                    MissingSessionCard {
                        supplementWindow = plannedWindow
                    }
                }
            }

            CheckInPhase.DAY_COMPLETE -> {
                item {
                    DayCompleteCard(state.latestCompleted) {
                        editingCompletedId = state.latestCompleted?.id
                    }
                }
            }
        }
    }

    supplementWindow?.let { window ->
        SessionTimeEditorDialog(
            title = "补记昨晚",
            description = "APP 不会自动猜测你的睡眠时间，请按实际情况确认。",
            initialSleepAt = window.bedtimeAt,
            initialWakeAt = window.wakeAt.coerceAtMost(now),
            onDismiss = { supplementWindow = null },
            onConfirm = { selectedSleepAt, selectedWakeAt ->
                viewModel.supplementSession(selectedSleepAt, selectedWakeAt)
                supplementWindow = null
            }
        )
    }

    state.completedSessions.firstOrNull { it.id == editingCompletedId }?.let { session ->
        session.wakeCheckInAt?.let { wakeAt ->
            SessionTimeEditorDialog(
                title = "纠正昨晚记录",
                description = "直接改数字，保存后会重新计算时长和手帐趋势。",
                initialSleepAt = session.sleepCheckInAt,
                initialWakeAt = wakeAt,
                onDismiss = { editingCompletedId = null },
                onConfirm = { selectedSleepAt, selectedWakeAt ->
                    viewModel.correctSession(session.id, selectedSleepAt, selectedWakeAt)
                    editingCompletedId = null
                },
                onRestore = if (session.wasCorrected) {
                    {
                        viewModel.restoreOriginalSession(session.id)
                        editingCompletedId = null
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun rememberMinuteClock(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    return now
}

@Composable
private fun CheckInTimeAdjustCard(
    sleepAt: Long,
    wakeAt: Long?,
    onSleepChange: () -> Unit,
    onWakeChange: (() -> Unit)? = null
) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = .22f)),
        border = BorderStroke(1.dp, Lavender.copy(alpha = .65f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("本次记录时间", color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            DateTimeEditRow("准备休息", sleepAt, onSleepChange)
            if (wakeAt != null && onWakeChange != null) {
                HorizontalDivider(color = Lavender.copy(alpha = .35f))
                DateTimeEditRow("实际起床", wakeAt, onWakeChange)
                Text(
                    "预计记录 ${formatDuration(((wakeAt - sleepAt).coerceAtLeast(0L)) / 60_000L)}",
                    color = Ink.copy(alpha = .62f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DateTimeEditRow(label: String, timestamp: Long, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = Ink, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Ink.copy(alpha = .62f), fontSize = 11.sp)
            Text(formatDateTime(timestamp), color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Rounded.Edit, contentDescription = "调整${label}", tint = Lavender, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MissingSessionCard(onSupplement: () -> Unit) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Blush.copy(alpha = .42f)),
        border = BorderStroke(1.dp, Blush)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("昨晚好像漏记了一页", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "补上实际准备休息和起床时间后，睡眠时长、偏离计划和手帐趋势会一起更新。",
                color = Ink.copy(alpha = .72f),
                fontSize = 13.sp
            )
            Button(onClick = onSupplement, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
                Spacer(Modifier.width(7.dp))
                Text("补记昨晚")
            }
            Text("没有确认前，APP 不会自动生成任何时间。", color = Ink.copy(alpha = .55f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun PreviousMissingCard(onSupplement: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Blush.copy(alpha = .24f)),
        border = BorderStroke(1.dp, Blush.copy(alpha = .72f)),
        modifier = Modifier.clickable(onClick = onSupplement)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = Ink)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text("昨晚还有一页没记", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("不影响今晚打卡，想起来时再补", color = Ink.copy(alpha = .62f), fontSize = 11.sp)
            }
            Text("去补记", color = Lavender, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DayCompleteCard(session: SleepSessionEntity?, onCorrect: () -> Unit) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Sage.copy(alpha = .18f)),
        border = BorderStroke(1.dp, Sage.copy(alpha = .72f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Sage, modifier = Modifier.size(38.dp))
            Text("昨晚已经记好了", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            session?.let {
                Text(
                    "${formatTime(it.sleepCheckInAt)} → ${it.wakeCheckInAt?.let(::formatTime) ?: "—"} · ${formatDuration(it.restWindowMinutes)}",
                    color = Ink.copy(alpha = .72f),
                    fontSize = 13.sp
                )
            }
            OutlinedButton(
                onClick = onCorrect,
                enabled = session?.wakeCheckInAt != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("时间不对？纠正昨晚记录")
            }
            Text("进入今晚的睡前时段后，这里会自动恢复睡前打卡。", color = Ink.copy(alpha = .55f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun SessionTimeEditorDialog(
    title: String,
    description: String,
    initialSleepAt: Long,
    initialWakeAt: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit,
    onRestore: (() -> Unit)? = null
) {
    val now = System.currentTimeMillis()
    var sleepInput by remember(initialSleepAt) { mutableStateOf(sessionClockInput(initialSleepAt)) }
    var wakeInput by remember(initialWakeAt) { mutableStateOf(sessionClockInput(initialWakeAt)) }
    val sleepAt = sessionClockTimestamp(sleepInput)
    val wakeAt = sessionClockTimestamp(wakeInput)
    val valid = sleepAt != null && wakeAt != null &&
        wakeAt > sleepAt && sleepAt <= now && wakeAt <= now
    val durationMinutes = if (sleepAt != null && wakeAt != null && wakeAt > sleepAt) {
        (wakeAt - sleepAt) / 60_000L
    } else {
        null
    }
    val unusualDuration = durationMinutes?.let { it !in 60L..(16L * 60L) } == true
    val validationText = when {
        sleepAt == null || wakeAt == null -> "请输入有效的 24 小时时间"
        wakeAt <= sleepAt -> "起床时间必须晚于准备休息时间"
        sleepAt > now || wakeAt > now -> "不能选择未来时间"
        else -> "调整后休息时长：${formatDuration(durationMinutes)}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(description, color = Ink.copy(alpha = .66f), fontSize = 12.sp)
                InlineDateTimeEditor(
                    label = "准备休息",
                    input = sleepInput,
                    onInputChange = { sleepInput = it }
                )
                InlineDateTimeEditor(
                    label = "实际起床",
                    input = wakeInput,
                    onInputChange = { wakeInput = it }
                )
                Text(
                    validationText,
                    color = if (valid) Ink.copy(alpha = .72f) else Blush,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (valid && unusualDuration) {
                    Text("这段时长比较少见，保存前请再确认一次。", color = Color(0xFFB77818), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    if (sleepAt != null && wakeAt != null) onConfirm(sleepAt, wakeAt)
                }
            ) {
                Text("确认保存")
            }
        },
        dismissButton = {
            Row {
                onRestore?.let {
                    TextButton(onClick = it) { Text("恢复原始时间") }
                }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
private fun InlineDateTimeEditor(
    label: String,
    input: SessionClockInput,
    onInputChange: (SessionClockInput) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = .17f)),
        border = BorderStroke(1.dp, Lavender.copy(alpha = .55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = { onInputChange(input.copy(date = input.date.minusDays(1))) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "${label}前一天",
                        tint = Ink
                    )
                }
                Text(
                    input.date.format(DateTimeFormatter.ofPattern("M月d日")),
                    color = Ink.copy(alpha = .72f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { onInputChange(input.copy(date = input.date.plusDays(1))) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "${label}后一天",
                        tint = Ink
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClockNumberField(
                    value = input.hourText,
                    label = "时",
                    onValueChange = { onInputChange(input.copy(hourText = it)) }
                )
                Text(
                    ":",
                    color = Ink,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 7.dp)
                )
                ClockNumberField(
                    value = input.minuteText,
                    label = "分",
                    onValueChange = { onInputChange(input.copy(minuteText = it)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(-60L to "−1时", -10L to "−10分", 10L to "+10分", 60L to "+1时").forEach { (minutes, text) ->
                    AssistChip(
                        onClick = { onInputChange(adjustSessionClock(input, minutes)) },
                        label = { Text(text, fontSize = 10.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockNumberField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            onValueChange(raw.filter { it.isDigit() }.take(2))
        },
        modifier = Modifier.width(76.dp),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = Ink,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun CheckInJournalHero(note: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(136.dp)
            .clip(cardShape)
            .background(Cream)
    ) {
        Image(
            painter = painterResource(R.drawable.checkin_journal_hero),
            contentDescription = "抱着枕头的睡眠帽玩偶",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.CenterEnd
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(12.dp)
                .fillMaxWidth(.50f)
                .clip(RoundedCornerShape(14.dp))
                .background(Blush.copy(alpha = .72f))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd · EEE")),
                color = Ink,
                fontSize = 10.sp
            )
            Text(note, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun CheckInJournalSheet(
    mood: String?,
    onMoodSelected: (String) -> Unit,
    selectedTags: Set<String>,
    onTagSelected: (String) -> Unit
) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            JournalSectionTitle("现在感觉怎么样？")
            MoodChoiceRow(mood = mood, onMoodSelected = onMoodSelected)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .25f))
            JournalSectionTitle("今晚发生了什么？", suffix = "可多选")
            bedtimeTagOptions.chunked(3).forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowOptions.forEach { option ->
                        TagChoiceButton(
                            modifier = Modifier.weight(1f),
                            option = option,
                            selected = option.label in selectedTags,
                            onClick = { onTagSelected(option.label) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodChoiceCard(
    title: String,
    mood: String?,
    onMoodSelected: (String) -> Unit
) {
    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Paper),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JournalSectionTitle(title)
            MoodChoiceRow(mood = mood, onMoodSelected = onMoodSelected)
        }
    }
}

@Composable
private fun JournalSectionTitle(title: String, suffix: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Lavender))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.weight(1f))
        suffix?.let { Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun MoodChoiceRow(mood: String?, onMoodSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        moodOptions.forEach { option ->
            OutlinedButton(
                onClick = { onMoodSelected(option.label) },
                modifier = Modifier.weight(1f).height(72.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (mood == option.label) Lavender.copy(alpha = .32f) else Color.Transparent,
                    contentColor = Ink
                ),
                border = BorderStroke(
                    if (mood == option.label) 2.dp else 1.dp,
                    if (mood == option.label) Lavender else MaterialTheme.colorScheme.outline.copy(alpha = .55f)
                ),
                contentPadding = PaddingValues(horizontal = 3.dp, vertical = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(option.icon, contentDescription = null, modifier = Modifier.size(22.dp))
                    Text(option.label, fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TagChoiceButton(
    modifier: Modifier,
    option: CheckInOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Sage.copy(alpha = .20f) else Color.Transparent,
            contentColor = Ink
        ),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) Sage else MaterialTheme.colorScheme.outline.copy(alpha = .5f)
        ),
        contentPadding = PaddingValues(horizontal = 3.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(option.label, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun PrimaryCheckInButton(
    text: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ActiveSessionCard(session: SleepSessionEntity) {
    Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = Ink)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("今晚已开始记录", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("睡前打卡 ${formatDateTime(session.sleepCheckInAt)}", color = Lavender)
            Text("早起打卡后，系统才会结算这段休息窗口。", color = Color.White.copy(alpha = .75f))
        }
    }
}

@Composable
private fun JournalScreen(
    sessions: List<SleepSessionEntity>,
    settings: AppSettings,
    viewModel: AppViewModel,
    snackbar: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showDataManagement by remember { mutableStateOf(false) }
    var showRecordDetails by remember { mutableStateOf(true) }
    var selectedSessionId by remember { mutableStateOf<Long?>(sessions.firstOrNull()?.id) }
    var editingSessionId by remember { mutableStateOf<Long?>(null) }
    var showCorrectionPicker by remember { mutableStateOf(false) }
    var rangePreset by remember { mutableStateOf(JournalRangePreset.RECENT_7) }
    var journalGrouping by remember { mutableStateOf(JournalGrouping.DAY) }
    var journalRange by remember { mutableStateOf(JournalDateRange.recentDays(7, "近7晚")) }
    var showCustomRangePicker by remember { mutableStateOf(false) }
    var exportPayload by remember { mutableStateOf("") }
    val sessionIds = remember(sessions) { sessions.map { it.id } }
    val journalPoints = remember(sessions, settings, journalRange, journalGrouping) {
        buildJournalRangePoints(sessions, settings, journalRange, journalGrouping)
    }
    val sessionsInRange = remember(sessions, journalRange) {
        countSessionsInRange(sessions, journalRange)
    }
    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { writeDocument(context, it, exportPayload, snackbar, scope) } }
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { writeDocument(context, it, exportPayload, snackbar, scope) } }

    LaunchedEffect(sessionIds) {
        val firstSessionId = sessionIds.firstOrNull()
        when {
            selectedSessionId == null -> selectedSessionId = firstSessionId
            selectedSessionId !in sessionIds -> selectedSessionId = firstSessionId
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            JournalHeader(
                dataManagementOpen = showDataManagement,
                canCorrect = sessions.any { it.wakeCheckInAt != null },
                onDataManagement = { showDataManagement = !showDataManagement },
                onCorrectHistory = { showCorrectionPicker = true }
            )
        }
        if (showDataManagement) {
            item {
                ExportCard(
                    onJson = {
                        scope.launch {
                            exportPayload = viewModel.jsonExport()
                            createJson.launch("sleep-saver-${LocalDate.now()}.json")
                        }
                    },
                    onCsv = {
                        scope.launch {
                            exportPayload = viewModel.csvExport()
                            createCsv.launch("sleep-saver-${LocalDate.now()}.csv")
                        }
                    }
                )
            }
        }
        item {
            JournalRangeControls(
                preset = rangePreset,
                range = journalRange,
                grouping = journalGrouping,
                completedNights = sessionsInRange,
                onPresetSelected = { selected ->
                    when (selected) {
                        JournalRangePreset.RECENT_7 -> {
                            rangePreset = selected
                            journalGrouping = JournalGrouping.DAY
                            journalRange = JournalDateRange.recentDays(7, selected.label)
                        }
                        JournalRangePreset.RECENT_30 -> {
                            rangePreset = selected
                            journalGrouping = JournalGrouping.WEEK
                            journalRange = JournalDateRange.recentDays(30, selected.label)
                        }
                        JournalRangePreset.CUSTOM -> {
                            showCustomRangePicker = true
                        }
                    }
                },
                onGroupingSelected = { journalGrouping = it }
            )
        }
        item { JournalAverageSleepChart(journalPoints, settings, journalGrouping) }
        item { JournalDeviationChart(journalPoints, journalGrouping) }
        item {
            JournalRecordDetailSection(
                sessions = sessions,
                expanded = showRecordDetails,
                selectedSessionId = selectedSessionId,
                onToggle = { showRecordDetails = !showRecordDetails },
                onSelectSession = { selectedSessionId = it },
                onEditSession = { editingSessionId = it }
            )
        }
    }

    if (showCustomRangePicker) {
        JournalCustomRangePicker(
            currentRange = journalRange,
            onDismiss = { showCustomRangePicker = false },
            onConfirm = { selectedRange ->
                rangePreset = JournalRangePreset.CUSTOM
                journalGrouping = JournalGrouping.WEEK
                journalRange = selectedRange.copy(label = JournalRangePreset.CUSTOM.label)
                showCustomRangePicker = false
            }
        )
    }

    if (showCorrectionPicker) {
        JournalCorrectionPickerDialog(
            sessions = sessions.filter { it.wakeCheckInAt != null },
            onDismiss = { showCorrectionPicker = false },
            onSelect = { sessionId ->
                showCorrectionPicker = false
                editingSessionId = sessionId
            }
        )
    }

    sessions.firstOrNull { it.id == editingSessionId }?.let { session ->
        session.wakeCheckInAt?.let { wakeAt ->
            SessionTimeEditorDialog(
                title = "调整睡眠记录",
                description = "修改后会重新计算时长和手帐趋势；首次记录时间会保留，可随时恢复。",
                initialSleepAt = session.sleepCheckInAt,
                initialWakeAt = wakeAt,
                onDismiss = { editingSessionId = null },
                onConfirm = { selectedSleepAt, selectedWakeAt ->
                    viewModel.correctSession(session.id, selectedSleepAt, selectedWakeAt)
                    editingSessionId = null
                },
                onRestore = if (session.wasCorrected) {
                    {
                        viewModel.restoreOriginalSession(session.id)
                        editingSessionId = null
                    }
                } else {
                    null
                }
            )
        }
    }
}

@Composable
private fun JournalHeader(
    dataManagementOpen: Boolean,
    canCorrect: Boolean,
    onDataManagement: () -> Unit,
    onCorrectHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("睡眠手帐", fontSize = 29.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("只记录，不评判", fontSize = 14.sp, color = Ink.copy(alpha = .64f))
            }
            OutlinedButton(
                onClick = onDataManagement,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE8B766)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Paper.copy(alpha = .82f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (dataManagementOpen) "收起" else "数据管理", fontSize = 12.sp)
            }
        }
        OutlinedButton(
            onClick = onCorrectHistory,
            enabled = canCorrect,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Lavender.copy(alpha = .82f)),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Lavender.copy(alpha = .14f))
        ) {
            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(if (canCorrect) "纠正历史记录" else "还没有可纠正的记录", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun JournalCorrectionPickerDialog(
    sessions: List<SleepSessionEntity>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择要纠正的记录", color = Ink, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(session.id) },
                        shape = RoundedCornerShape(13.dp),
                        colors = CardDefaults.cardColors(containerColor = Paper),
                        border = BorderStroke(
                            1.dp,
                            if (sessionNeedsCorrectionAttention(session)) Blush else Lavender.copy(alpha = .45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(formatJournalDate(session.sessionDate), color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    "${formatTime(session.sleepCheckInAt)} → ${session.wakeCheckInAt?.let(::formatTime) ?: "—"} · ${formatDuration(session.restWindowMinutes)}",
                                    color = Ink.copy(alpha = .66f),
                                    fontSize = 11.sp
                                )
                            }
                            if (session.wasCorrected) {
                                Text("已调整", color = Sage, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(7.dp))
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "纠正这晚", tint = Lavender)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun JournalRangeControls(
    preset: JournalRangePreset,
    range: JournalDateRange,
    grouping: JournalGrouping,
    completedNights: Int,
    onPresetSelected: (JournalRangePreset) -> Unit,
    onGroupingSelected: (JournalGrouping) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            JournalSegmentedControl(
                values = JournalRangePreset.entries,
                selected = preset,
                label = { it.label },
                onSelected = onPresetSelected,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = Ink.copy(alpha = .72f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${formatRangeDate(range.start)}—${formatRangeDate(range.endInclusive)} · ${completedNights}晚",
                    color = Ink.copy(alpha = .82f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            JournalSegmentedControl(
                values = JournalGrouping.entries,
                selected = grouping,
                label = { it.label },
                onSelected = onGroupingSelected,
                modifier = Modifier.width(258.dp)
            )
        }
        Image(
            painter = painterResource(R.drawable.today_timeline_mascot),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-24).dp, y = (-48).dp)
                .size(68.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun <T> JournalSegmentedControl(
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(42.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Paper.copy(alpha = .92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFE8DDC8))
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            values.forEachIndexed { index, value ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(if (value == selected) Lavender.copy(alpha = .42f) else Color.Transparent)
                        .clickable { onSelected(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label(value),
                        color = Ink,
                        fontSize = 13.sp,
                        fontWeight = if (value == selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
                if (index < values.lastIndex) {
                    Box(Modifier.width(1.dp).height(22.dp).background(Color(0xFFE8DDC8)))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalCustomRangePicker(
    currentRange: JournalDateRange,
    onDismiss: () -> Unit,
    onConfirm: (JournalDateRange) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = journalDateToUtcMillis(currentRange.start),
        initialSelectedEndDateMillis = journalDateToUtcMillis(currentRange.endInclusive)
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = journalDateRangeCanConfirm(
                    pickerState.selectedStartDateMillis,
                    pickerState.selectedEndDateMillis
                ),
                onClick = {
                    val startMillis = pickerState.selectedStartDateMillis
                    val endMillis = pickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        val start = journalDateFromUtcMillis(startMillis)
                        val end = journalDateFromUtcMillis(endMillis)
                        onConfirm(
                            JournalDateRange(
                                start = start,
                                endInclusive = end,
                                label = "${start.monthValue}/${start.dayOfMonth}–${end.monthValue}/${end.dayOfMonth}"
                            )
                        )
                    }
                }
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    text = "选择统计日期",
                    modifier = Modifier.padding(start = 24.dp, top = 18.dp)
                )
            },
            headline = {
                Text(
                    text = if (pickerState.selectedEndDateMillis == null) {
                        "再选择结束日期"
                    } else {
                        "已选择日期范围"
                    },
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 10.dp)
                )
            },
            showModeToggle = false
        )
    }
}

internal fun journalDateRangeCanConfirm(startMillis: Long?, endMillis: Long?): Boolean =
    startMillis != null && endMillis != null

private val journalCalendarZone: ZoneId = ZoneId.of("UTC")

internal fun journalDateToUtcMillis(date: LocalDate): Long =
    date.atStartOfDay(journalCalendarZone).toInstant().toEpochMilli()

internal fun journalDateFromUtcMillis(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(journalCalendarZone).toLocalDate()

@Composable
private fun JournalAverageSleepChart(
    points: List<JournalWeekPoint>,
    settings: AppSettings,
    grouping: JournalGrouping
) {
    val scrollState = rememberScrollState()
    val trackTop = 30.dp
    val trackHeight = 102.dp
    val targetRatio = (settings.targetSleepMinutes / 540f).coerceIn(0.12f, 1f)
    val targetLineY = trackTop + trackHeight * (1f - targetRatio)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 7.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0FA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, Color(0xFFE8E0EF))
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 17.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                JournalChartTitle("平均睡眠时长")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                ) {
                    val viewportWidth = maxWidth
                    val chartWidth = maxOf(viewportWidth, 45.dp * points.size.coerceAtLeast(1))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(chartWidth)
                                .height(176.dp)
                        ) {
                            val density = LocalDensity.current
                            Canvas(Modifier.fillMaxSize()) {
                                drawLine(
                                    color = Sage.copy(alpha = .78f),
                                    start = Offset(0f, with(density) { targetLineY.toPx() }),
                                    end = Offset(size.width - with(density) { 30.dp.toPx() }, with(density) { targetLineY.toPx() }),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                )
                                drawLine(
                                    color = Ink.copy(alpha = .24f),
                                    start = Offset(with(density) { 6.dp.toPx() }, with(density) { 145.dp.toPx() }),
                                    end = Offset(size.width - with(density) { 6.dp.toPx() }, with(density) { 145.dp.toPx() }),
                                    strokeWidth = 2f
                                )
                            }
                            Text(
                                "${settings.targetSleepMinutes / 60}小时",
                                color = Sage,
                                fontSize = 9.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(y = targetLineY - 10.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 4.dp, end = 27.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Top
                            ) {
                                points.forEachIndexed { index, point ->
                                    JournalAverageSleepColumn(
                                        point = point,
                                        tapeResource = journalDurationTapeResources[index % journalDurationTapeResources.size],
                                        trackHeight = trackHeight,
                                        grouping = grouping
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.journal_tape_gingham),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-15).dp, y = (-8).dp)
                .width(66.dp)
                .height(34.dp),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun JournalChartTitle(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).background(Lavender, CircleShape))
        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JournalAverageSleepColumn(
    point: JournalWeekPoint,
    tapeResource: Int,
    trackHeight: androidx.compose.ui.unit.Dp,
    grouping: JournalGrouping
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.width(40.dp)
    ) {
        Text(
            point.averageSleepMinutes?.let { formatClockDuration(it.toLong()) } ?: "—",
            color = Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
        JournalDurationTapeBar(point.averageSleepMinutes, tapeResource, trackHeight)
        Text(
            journalAxisLabel(point.periodStart, grouping),
            color = Ink.copy(alpha = .78f),
            fontSize = 9.sp
        )
    }
}

@Composable
private fun JournalDurationTapeBar(
    minutes: Int?,
    tapeResource: Int,
    trackHeight: androidx.compose.ui.unit.Dp
) {
    val fillFraction = minutes?.let { (it / 540f).coerceIn(0.16f, 1f) } ?: 0f
    val fillHeight = trackHeight * fillFraction
    Box(
        modifier = Modifier
            .width(30.dp)
            .height(trackHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        if (minutes == null) {
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(2.dp)
                    .background(Ink.copy(alpha = .12f), RoundedCornerShape(2.dp))
            )
        } else {
            Image(
                painter = painterResource(tapeResource),
                contentDescription = null,
                modifier = Modifier
                    .width(27.dp)
                    .height(fillHeight),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

@Composable
private fun JournalDeviationChart(
    points: List<JournalWeekPoint>,
    grouping: JournalGrouping
) {
    val maxDeviation = max(
        90,
        points.mapNotNull { it.averageDeviationMinutes?.let(::abs) }.maxOrNull() ?: 90
    )
    val chartRange = ((maxDeviation + 29) / 30) * 30
    val scrollState = rememberScrollState()
    val sidePadding = JOURNAL_DEVIATION_SIDE_PADDING_DP.dp
    val zeroLineY = 58.dp
    val pointAmplitude = 12.dp
    val ribbonTop = 92.dp
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 7.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0FA)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, Color(0xFFE8E0EF))
        ) {
            Column(
                modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 14.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                JournalChartTitle("平均偏离计划")
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                ) {
                    val viewportWidth = maxWidth
                    val viewportWidthDp = viewportWidth.value.roundToInt()
                    val chartWidthDp = journalDeviationChartWidthDp(viewportWidthDp, points.size)
                    val chartWidth = chartWidthDp.dp
                    val shouldPinLatest = chartWidthDp > viewportWidthDp
                    LaunchedEffect(
                        shouldPinLatest,
                        scrollState.maxValue,
                        points.firstOrNull()?.periodStart,
                        points.lastOrNull()?.periodStart,
                        grouping
                    ) {
                        if (shouldPinLatest && scrollState.maxValue > 0) {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(scrollState)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(chartWidth)
                                .height(148.dp)
                        ) {
                            val step = if (points.size > 1) {
                                (chartWidth - sidePadding * 2) / (points.size - 1)
                            } else {
                                0.dp
                            }
                            val pointXs = if (points.size == 1) {
                                listOf(chartWidth / 2)
                            } else {
                                points.indices.map { sidePadding + step * it }
                            }

                            Canvas(Modifier.fillMaxSize()) {
                                drawLine(
                                    color = Lavender.copy(alpha = .34f),
                                    start = Offset(0f, with(density) { zeroLineY.toPx() }),
                                    end = Offset(size.width, with(density) { zeroLineY.toPx() }),
                                    strokeWidth = 2f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))
                                )

                                for (index in 0 until points.lastIndex) {
                                    val startDeviation = points[index].averageDeviationMinutes
                                    val endDeviation = points[index + 1].averageDeviationMinutes
                                    if (startDeviation != null && endDeviation != null) {
                                        val start = Offset(
                                            with(density) { pointXs[index].toPx() },
                                            with(density) {
                                                (zeroLineY - pointAmplitude *
                                                    (startDeviation.toFloat() / chartRange.toFloat())).toPx()
                                            }
                                        )
                                        val end = Offset(
                                            with(density) { pointXs[index + 1].toPx() },
                                            with(density) {
                                                (zeroLineY - pointAmplitude *
                                                    (endDeviation.toFloat() / chartRange.toFloat())).toPx()
                                            }
                                        )
                                        drawLine(
                                            color = Lavender,
                                            start = start,
                                            end = end,
                                            strokeWidth = 5f,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }

                                points.forEachIndexed { index, point ->
                                    point.averageDeviationMinutes?.let { deviation ->
                                        val y = zeroLineY - pointAmplitude *
                                            (deviation.toFloat() / chartRange.toFloat())
                                        drawLine(
                                            color = Color(0xFFD7AAA7),
                                            start = Offset(
                                                with(density) { pointXs[index].toPx() },
                                                with(density) { y.toPx() + 12.dp.toPx() }
                                            ),
                                            end = Offset(
                                                with(density) { pointXs[index].toPx() },
                                                with(density) { ribbonTop.toPx() }
                                            ),
                                            strokeWidth = 1.5f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .offset(y = ribbonTop)
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .padding(horizontal = 3.dp)
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.journal_plan_ribbon),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.FillBounds
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("计划附近", color = Ink.copy(alpha = .78f), fontSize = 9.sp)
                                    Text("±30分钟", color = Ink.copy(alpha = .62f), fontSize = 9.sp)
                                }
                            }

                            points.forEachIndexed { index, point ->
                                val pointY = point.averageDeviationMinutes?.let {
                                    zeroLineY - pointAmplitude * (it.toFloat() / chartRange.toFloat())
                                } ?: zeroLineY
                                point.averageDeviationMinutes?.let { deviation ->
                                    JournalDeviationLabel(
                                        label = formatDeviationLabel(deviation),
                                        modifier = Modifier.offset(
                                            x = pointXs[index] - (JOURNAL_DEVIATION_LABEL_WIDTH_DP / 2).dp,
                                            y = pointY - JOURNAL_DEVIATION_LABEL_TOP_OFFSET_DP.dp
                                        )
                                    )
                                    JournalDeviationPoint(
                                        modifier = Modifier.offset(
                                            x = pointXs[index] - 14.dp,
                                            y = pointY - 14.dp
                                        )
                                    )
                                } ?: Text(
                                    "—",
                                    color = Ink.copy(alpha = .24f),
                                    fontSize = 10.sp,
                                    modifier = Modifier.offset(
                                        x = pointXs[index] - 3.dp,
                                        y = zeroLineY - 9.dp
                                    )
                                )
                                Text(
                                    journalAxisLabel(point.periodStart, grouping),
                                    color = Ink.copy(alpha = .78f),
                                    fontSize = 9.sp,
                                    modifier = Modifier.offset(
                                        x = pointXs[index] - 13.dp,
                                        y = 128.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        Image(
            painter = painterResource(R.drawable.journal_tape_polka),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 9.dp, y = (-10).dp)
                .width(70.dp)
                .height(34.dp),
            contentScale = ContentScale.Fit
        )
    }
}

internal const val JOURNAL_DEVIATION_SIDE_PADDING_DP = 18
internal const val JOURNAL_DEVIATION_POINT_SPACING_DP = 42
internal const val JOURNAL_DEVIATION_LABEL_WIDTH_DP = 34
internal const val JOURNAL_DEVIATION_LABEL_HEIGHT_DP = 25
internal const val JOURNAL_DEVIATION_LABEL_TOP_OFFSET_DP = 44
internal const val JOURNAL_DEVIATION_MASCOT_HALF_SIZE_DP = 16
internal const val JOURNAL_DEVIATION_INLINE_POINT_LIMIT = 7
internal const val JOURNAL_DEVIATION_INLINE_GAP_DP = 4

internal fun journalDeviationChartWidthDp(viewportWidthDp: Int, pointCount: Int): Int {
    val safePointCount = pointCount.coerceAtLeast(1)
    val inlineMinimumWidth = JOURNAL_DEVIATION_SIDE_PADDING_DP * 2 +
        (JOURNAL_DEVIATION_LABEL_WIDTH_DP + JOURNAL_DEVIATION_INLINE_GAP_DP) *
        (safePointCount - 1)
    if (
        safePointCount <= JOURNAL_DEVIATION_INLINE_POINT_LIMIT &&
        viewportWidthDp >= inlineMinimumWidth
    ) {
        return viewportWidthDp
    }
    return maxOf(
        viewportWidthDp,
        JOURNAL_DEVIATION_SIDE_PADDING_DP * 2 +
        JOURNAL_DEVIATION_POINT_SPACING_DP * (safePointCount - 1)
    )
}

@Composable
private fun JournalDeviationLabel(label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(JOURNAL_DEVIATION_LABEL_WIDTH_DP.dp)
            .height(JOURNAL_DEVIATION_LABEL_HEIGHT_DP.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.journal_deviation_label),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp, end = 2.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Ink,
                fontSize = 7.5.sp,
                lineHeight = 8.5.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun JournalDeviationPoint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.journal_sleepy_mascot),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit
        )
    }
}

internal const val JOURNAL_HISTORY_DATE_CHIP_WIDTH_DP = 58
internal const val JOURNAL_HISTORY_DATE_CHIP_HEIGHT_DP = 44
internal const val JOURNAL_HISTORY_EDIT_BUTTON_HEIGHT_DP = 48

@Composable
private fun JournalRecordDetailSection(
    sessions: List<SleepSessionEntity>,
    expanded: Boolean,
    selectedSessionId: Long?,
    onToggle: () -> Unit,
    onSelectSession: (Long) -> Unit,
    onEditSession: (Long) -> Unit
) {
    val selectedSession = selectedJournalSession(sessions, selectedSessionId)
    val selectedIndex = selectedSession
        ?.let { selected -> sessions.indexOfFirst { it.id == selected.id } }
        ?.takeIf { it >= 0 }
        ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (sessions.isEmpty()) {
            GentleNoteCard()
        } else {
            Card(
                modifier = Modifier
                    .width(212.dp)
                    .height(42.dp)
                    .clickable(onClick = onToggle),
                shape = RoundedCornerShape(13.dp),
                colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = .34f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, Color(0xFFE3A958))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text("睡眠记录详情", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text("${sessions.size}晚", color = Ink.copy(alpha = .62f), fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Icon(
                        if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = if (expanded) "收起睡眠详情" else "展开睡眠详情",
                        tint = Ink,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            AnimatedVisibility(expanded && selectedSession != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .offset(y = 9.dp)
                            .background(Paper, RoundedCornerShape(3.dp))
                            .border(
                                BorderStroke(1.dp, Lavender.copy(alpha = .28f)),
                                RoundedCornerShape(3.dp)
                            )
                    ) {
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(.92f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Paper.copy(alpha = .96f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
                        border = BorderStroke(1.dp, Lavender.copy(alpha = .22f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            JournalRecordNavigator(
                                sessions = sessions,
                                selectedIndex = selectedIndex,
                                onSelectSession = onSelectSession
                            )
                            selectedSession?.let {
                                OutlinedButton(
                                    onClick = { onEditSession(it.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(JOURNAL_HISTORY_EDIT_BUTTON_HEIGHT_DP.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Lavender.copy(alpha = .82f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Edit,
                                        contentDescription = null,
                                        tint = Ink,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "修改 ${formatMonthDayChip(it.sessionDate)} 这晚",
                                        color = Ink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                JournalHistoryPaper(
                                    session = it,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalRecordNavigator(
    sessions: List<SleepSessionEntity>,
    selectedIndex: Int,
    onSelectSession: (Long) -> Unit
) {
    val visibleSessions = detailWindowSessions(sessions, selectedIndex)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            enabled = selectedIndex > 0,
            onClick = { sessions.getOrNull(selectedIndex - 1)?.let { onSelectSession(it.id) } }
        ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "上一晚", tint = Ink)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            visibleSessions.forEach { session ->
                val selected = session.id == sessions.getOrNull(selectedIndex)?.id
                Box(
                    modifier = Modifier
                        .width(JOURNAL_HISTORY_DATE_CHIP_WIDTH_DP.dp)
                        .height(JOURNAL_HISTORY_DATE_CHIP_HEIGHT_DP.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Lavender else Paper)
                        .border(
                            BorderStroke(1.dp, if (selected) Lavender else Ink.copy(alpha = .10f)),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectSession(session.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatMonthDayChip(session.sessionDate),
                        color = if (selected) Color.White else Ink,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
        IconButton(
            enabled = selectedIndex < sessions.lastIndex,
            onClick = { sessions.getOrNull(selectedIndex + 1)?.let { onSelectSession(it.id) } }
        ) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "下一晚", tint = Ink)
        }
    }
}

internal fun selectedJournalSession(
    sessions: List<SleepSessionEntity>,
    selectedSessionId: Long?
): SleepSessionEntity? =
    sessions.firstOrNull { it.id == selectedSessionId } ?: sessions.firstOrNull()

private fun detailWindowSessions(
    sessions: List<SleepSessionEntity>,
    selectedIndex: Int,
    maxCount: Int = 3
): List<SleepSessionEntity> {
    if (sessions.size <= maxCount) return sessions
    val safeIndex = selectedIndex.coerceIn(0, sessions.lastIndex)
    val start = max(0, min(safeIndex - 1, sessions.size - maxCount))
    return sessions.subList(start, start + maxCount)
}

@Composable
private fun JournalHistoryPaper(
    session: SleepSessionEntity,
    modifier: Modifier = Modifier
) {
    val mood = session.moodAfterWake ?: session.moodBeforeSleep
    val tag = session.tags().firstOrNull()
    val recordMarker = when {
        session.isSupplemented && session.wasCorrected -> "补记 · 已调整"
        session.isSupplemented -> "补记"
        session.wasCorrected -> "已调整"
        else -> null
    }
    val footer = listOfNotNull(recordMarker, historyUsageText(session)).joinToString(" · ")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(184.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.journal_history_paper),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                formatJournalDate(session.sessionDate),
                color = Ink,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NightsStay, contentDescription = null, tint = Ink, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    "${formatTime(session.sleepCheckInAt)} → ${session.wakeCheckInAt?.let(::formatTime) ?: "未完成"}",
                    color = Ink,
                    fontSize = 13.sp
                )
            }
            Text(formatDuration(session.restWindowMinutes), color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mood?.let { JournalStamp(it, moodIcon(it), Lavender) }
                tag?.let { JournalStamp(it, tagIcon(it), Sage) }
            }
            Spacer(Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFFE8D9BE))
            Text(footer, color = Ink.copy(alpha = .62f), fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun JournalStamp(label: String, icon: ImageVector, color: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = .9f)),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = Ink, fontSize = 10.sp, maxLines = 1)
        }
    }
}

private fun buildJournalRangePoints(
    sessions: List<SleepSessionEntity>,
    settings: AppSettings,
    range: JournalDateRange,
    grouping: JournalGrouping
): List<JournalWeekPoint> {
    val start = minOf(range.start, range.endInclusive)
    val endInclusive = maxOf(range.start, range.endInclusive)
    val endExclusive = endInclusive.plusDays(1)
    val datedSessions = sessions.mapNotNull { session ->
        runCatching { LocalDate.parse(session.sessionDate) }.getOrNull()?.let { it to session }
    }
    val periods = buildJournalPeriods(start, endExclusive, grouping)

    return periods.map { (bucketStart, bucketEndExclusive) ->
        val bucketSessions = datedSessions
            .filter { (date, _) -> date >= bucketStart && date < bucketEndExclusive }
            .map { it.second }
        val averageSleepMinutes = bucketSessions
            .mapNotNull { it.restWindowMinutes?.toInt() }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
        val averageDeviationMinutes = bucketSessions
            .mapNotNull { bedtimeDeviationMinutes(it, settings) }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.roundToInt()
        JournalWeekPoint(bucketStart, averageSleepMinutes, averageDeviationMinutes)
    }
}

private fun buildJournalPeriods(
    start: LocalDate,
    endExclusive: LocalDate,
    grouping: JournalGrouping
): List<Pair<LocalDate, LocalDate>> {
    val periods = mutableListOf<Pair<LocalDate, LocalDate>>()
    var cursor = start
    while (cursor < endExclusive) {
        val next = when (grouping) {
            JournalGrouping.DAY -> cursor.plusDays(1)
            JournalGrouping.WEEK -> cursor.plusDays(7)
            JournalGrouping.MONTH -> YearMonth.from(cursor).plusMonths(1).atDay(1)
        }.coerceAtMost(endExclusive)
        periods += cursor to next
        cursor = next
    }
    return periods.ifEmpty { listOf(start to endExclusive) }
}

private fun countSessionsInRange(
    sessions: List<SleepSessionEntity>,
    range: JournalDateRange
): Int {
    val start = minOf(range.start, range.endInclusive)
    val endInclusive = maxOf(range.start, range.endInclusive)
    return sessions.count { session ->
        runCatching { LocalDate.parse(session.sessionDate) }
            .getOrNull()
            ?.let { it >= start && it <= endInclusive }
            ?: false
    }
}

private fun formatRangeDate(date: LocalDate): String =
    "${date.monthValue}月${date.dayOfMonth}日"

private fun journalAxisLabel(date: LocalDate, grouping: JournalGrouping): String = when (grouping) {
    JournalGrouping.MONTH -> "${date.monthValue}月"
    JournalGrouping.DAY,
    JournalGrouping.WEEK -> "${date.monthValue}/${date.dayOfMonth}"
}

private fun bedtimeDeviationMinutes(
    session: SleepSessionEntity,
    settings: AppSettings
): Int? {
    val plannedHour = session.plannedBedtimeHour ?: settings.bedtimeHour
    val plannedMinute = session.plannedBedtimeMinute ?: settings.bedtimeMinute
    val actualTime = Instant.ofEpochMilli(session.sleepCheckInAt)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
    var difference = actualTime.hour * 60 + actualTime.minute - (plannedHour * 60 + plannedMinute)
    if (difference <= -12 * 60) difference += 24 * 60
    if (difference > 12 * 60) difference -= 24 * 60
    return difference
}

private fun formatDeviationLabel(deviationMinutes: Int): String {
    val absValue = abs(deviationMinutes)
    val prefix = if (deviationMinutes >= 0) "晚" else "早"
    return "$prefix${absValue / 60}:${(absValue % 60).toString().padStart(2, '0')}"
}

private fun formatMonthDayChip(date: String): String = runCatching {
    val parsed = LocalDate.parse(date)
    "${parsed.monthValue}/${parsed.dayOfMonth}"
}.getOrDefault(date)

private fun formatClockDuration(minutes: Long): String =
    "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}"

private fun chineseWeekday(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}

private fun formatJournalDate(date: String): String = runCatching {
    val parsed = LocalDate.parse(date)
    "${parsed.monthValue}月${parsed.dayOfMonth}日  ${chineseWeekday(parsed)}"
}.getOrDefault(date)

private fun moodIcon(label: String): ImageVector = moodOptions.firstOrNull { it.label == label }?.icon
    ?: Icons.Rounded.SentimentNeutral

private fun tagIcon(label: String): ImageVector = bedtimeTagOptions.firstOrNull { it.label == label }?.icon
    ?: Icons.Rounded.MenuBook

private fun historyUsageText(session: SleepSessionEntity): String = when (session.usageDataAvailable) {
    true -> "夜间解锁 ${session.nightUnlockCount ?: 0} 次 · 本机睡前使用 ${session.preSleepPhoneMinutes ?: 0} 分钟"
    false -> "本机使用数据不可用"
    null -> "本机使用数据未记录"
}

@Composable
private fun FriendsUnavailableScreen() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = .5f))) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(Ink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.People, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Text("好友功能暂未开放", fontWeight = FontWeight.Bold, fontSize = 24.sp, textAlign = TextAlign.Center)
                Text(
                    "这一版先把自己的睡眠照顾好。好友、排行榜和云同步不会偷偷连接外部服务器。",
                    textAlign = TextAlign.Center
                )
                AssistChip(onClick = {}, label = { Text("敬请期待") })
            }
        }
    }
}

private fun greeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..10 -> "早安"
    in 11..17 -> "今天过得怎么样"
    else -> "晚上好"
}

private fun formatDuration(minutes: Long?): String {
    if (minutes == null) return "—"
    return "${minutes / 60} 小时 ${minutes % 60} 分"
}

private fun formatTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("HH:mm"))

private fun formatDateTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))

private fun showDateTimePicker(
    context: Context,
    initialTimestamp: Long,
    maxTimestamp: Long,
    onSelected: (Long) -> Unit
) {
    val zone = ZoneId.systemDefault()
    val initial = Instant.ofEpochMilli(initialTimestamp).atZone(zone)
    val dialog = android.app.DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val selected = LocalDate.of(year, month + 1, day)
                        .atTime(hour, minute)
                        .atZone(zone)
                        .toInstant()
                        .toEpochMilli()
                    onSelected(selected)
                },
                initial.hour,
                initial.minute,
                true
            ).show()
        },
        initial.year,
        initial.monthValue - 1,
        initial.dayOfMonth
    )
    dialog.datePicker.maxDate = maxTimestamp
    dialog.show()
}

private fun showTimePicker(
    context: Context,
    initialHour: Int,
    initialMinute: Int,
    onSelected: (Int, Int) -> Unit
) {
    TimePickerDialog(context, { _, hour, minute -> onSelected(hour, minute) }, initialHour, initialMinute, true).show()
}

private fun openUsageAccessSettings(context: Context) {
    val appSpecific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(appSpecific) }
        .recoverCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
}

private fun writeDocument(
    context: Context,
    uri: Uri,
    payload: String,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    scope.launch {
        val saved = runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(payload) }
                ?: error("无法打开文件")
        }.isSuccess
        snackbar.showSnackbar(if (saved) "数据已导出" else "导出失败，请重新选择位置")
    }
}
