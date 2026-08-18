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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepsaver.app.data.AppSettings
import com.sleepsaver.app.data.SleepSessionEntity
import com.sleepsaver.app.domain.SessionPolicy
import com.sleepsaver.app.ui.theme.Blush
import com.sleepsaver.app.ui.theme.Ink
import com.sleepsaver.app.ui.theme.Lavender
import com.sleepsaver.app.ui.theme.Sage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private enum class AppTab(val title: String, val icon: ImageVector) {
    TODAY("今日", Icons.Rounded.Home),
    CHECK_IN("打卡", Icons.Rounded.NightsStay),
    JOURNAL("手帐", Icons.Rounded.MenuBook),
    FRIENDS("好友", Icons.Rounded.People)
}

private val cardShape = RoundedCornerShape(22.dp)

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
                AppTab.JOURNAL -> JournalScreen(state.completedSessions)
                AppTab.FRIENDS -> FriendsUnavailableScreen()
            }
        }
    }
}

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var exportPayload by remember { mutableStateOf("") }

    val createJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { writeDocument(context, it, exportPayload, snackbar, scope) } }
    val createCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { writeDocument(context, it, exportPayload, snackbar, scope) } }

    var enableReminderAfterPermission by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && enableReminderAfterPermission) viewModel.setReminderEnabled(true)
        else if (!granted) scope.launch { snackbar.showSnackbar("未获得通知权限，提醒没有开启") }
        enableReminderAfterPermission = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenTitle(greeting(), LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · EEEE")))
        }
        if (!state.usagePermissionGranted) {
            item { UsagePermissionCard { openUsageAccessSettings(context) } }
        }
        item { SleepSummaryCard(state.activeSession, state.latestCompleted) }
        item { WeekProgressCard(state) }
        item { GentleNoteCard() }
        item {
            SleepScheduleCard(
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
private fun SleepSummaryCard(active: SleepSessionEntity?, latest: SleepSessionEntity?) {
    Card(shape = cardShape, colors = CardDefaults.cardColors(containerColor = Ink)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (active != null) {
                Text("今晚已开始记录", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(formatDateTime(active.sleepCheckInAt), color = Lavender, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("早起后请到“打卡”完成结算", color = Color.White.copy(alpha = .8f))
            } else if (latest != null) {
                Text("最近一次 · 两次打卡之间", color = Color.White.copy(alpha = .75f))
                Text(formatDuration(latest.restWindowMinutes), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("睡前打卡", formatTime(latest.sleepCheckInAt))
                    Metric("早起打卡", latest.wakeCheckInAt?.let(::formatTime) ?: "—")
                    Metric("夜间解锁", latest.nightUnlockCount?.let { "$it 次" } ?: "—")
                }
                if (latest.usageDataAvailable == false) {
                    Text("昨晚手机使用数据不可用", color = Blush)
                }
            } else {
                Text("今晚，从一次轻轻的打卡开始", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("数据只存在这台手机里。", color = Color.White.copy(alpha = .75f))
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = .6f), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WeekProgressCard(state: AppUiState) {
    val target = state.settings.targetSleepMinutes * 7L
    val progress = if (target == 0L) 0f else (state.weekRestMinutes.toFloat() / target).coerceIn(0f, 1f)
    Card(shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("本周目标", fontWeight = FontWeight.Bold)
                Text("${state.weekRestMinutes / 60}/${target / 60} 小时")
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text("目标 7 小时/晚 · 记录的是两次打卡之间的休息窗口", fontSize = 12.sp)
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
private fun SleepScheduleCard(
    settings: AppSettings,
    onBedtime: (Int, Int) -> Unit,
    onWakeTime: (Int, Int) -> Unit,
    onReminderChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Card(shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccessTime, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("睡眠定时", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(checked = settings.reminderEnabled, onCheckedChange = onReminderChange)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeButton(
                    modifier = Modifier.weight(1f),
                    label = "就寝",
                    hour = settings.bedtimeHour,
                    minute = settings.bedtimeMinute
                ) {
                    showTimePicker(context, settings.bedtimeHour, settings.bedtimeMinute, onBedtime)
                }
                TimeButton(
                    modifier = Modifier.weight(1f),
                    label = "起床",
                    hour = settings.wakeHour,
                    minute = settings.wakeMinute
                ) {
                    showTimePicker(context, settings.wakeHour, settings.wakeMinute, onWakeTime)
                }
            }
            Text("每晚重复 · 提前 ${settings.reminderAdvanceMinutes} 分钟提醒", fontSize = 12.sp)
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
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp)
            Text("%02d:%02d".format(hour, minute), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
    }
}

@Composable
private fun ExportCard(onJson: () -> Unit, onCsv: () -> Unit) {
    Card(shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("导出我的数据", fontWeight = FontWeight.Bold)
            }
            Text("导出时由你选择保存位置；应用不会自行读取其他文件。", fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onJson) { Text("导出 JSON") }
                OutlinedButton(onClick = onCsv) { Text("导出 CSV") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CheckInScreen(state: AppUiState, viewModel: AppViewModel) {
    val context = LocalContext.current
    var mood by remember(state.activeSession?.id) { mutableStateOf<String?>(null) }
    var selectedTags by remember(state.activeSession?.id) { mutableStateOf(setOf<String>()) }
    val beforeMoods = listOf("超累", "有点累", "还行", "精神", "清醒")
    val wakeMoods = listOf("精神", "还行", "有点累", "很困")
    val tags = listOf("加班了", "刷手机", "想事情", "喝了咖啡", "运动了", "今天很平静")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenTitle(
                if (state.activeSession == null) "睡前打卡" else "早起打卡",
                if (state.activeSession == null) "告诉自己：今天到这里就好" else "记录醒来时真实的感觉"
            )
        }
        if (!state.usagePermissionGranted) {
            item { UsagePermissionCard { openUsageAccessSettings(context) } }
        }
        if (state.activeSession == null) {
            item {
                ChoiceCard("现在感觉怎么样？") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        beforeMoods.forEach { label ->
                            FilterChip(selected = mood == label, onClick = { mood = label }, label = { Text(label) })
                        }
                    }
                }
            }
            item {
                ChoiceCard("今晚发生了什么？可多选") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { label ->
                            FilterChip(
                                selected = label in selectedTags,
                                onClick = {
                                    selectedTags = if (label in selectedTags) selectedTags - label else selectedTags + label
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.startSleep(mood, selectedTags.toList()) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.NightsStay, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("我要睡了 · 开始记录")
                }
            }
        } else {
            item { ActiveSessionCard(state.activeSession) }
            item {
                ChoiceCard("醒来感觉") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        wakeMoods.forEach { label ->
                            FilterChip(selected = mood == label, onClick = { mood = label }, label = { Text(label) })
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.finishWake(mood) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("完成早起打卡")
                }
            }
            if (SessionPolicy.canUndo(state.activeSession, System.currentTimeMillis())) {
                item {
                    OutlinedButton(onClick = viewModel::undoSleepCheckIn, modifier = Modifier.fillMaxWidth()) {
                        Text("手滑了，撤销睡前打卡（5 分钟内）")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(title: String, content: @Composable () -> Unit) {
    Card(shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
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
private fun JournalScreen(sessions: List<SleepSessionEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenTitle("睡眠手帐", "只记录，不评判") }
        item { WeekBars(sessions.take(7).reversed()) }
        if (sessions.isEmpty()) {
            item { GentleNoteCard() }
        } else {
            items(sessions, key = { it.id }) { session -> HistoryCard(session) }
        }
    }
}

@Composable
private fun WeekBars(sessions: List<SleepSessionEntity>) {
    Card(shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("最近 7 次休息窗口", fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth().height(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                sessions.forEach { session ->
                    val hours = (session.restWindowMinutes ?: 0L) / 60f
                    val barHeight = (hours / 10f * 110f).coerceIn(8f, 110f)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .width(24.dp)
                                .height(barHeight.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (session == sessions.lastOrNull()) Blush else Sage)
                        )
                        Text(session.sessionDate.takeLast(2), fontSize = 11.sp)
                    }
                }
                if (sessions.isEmpty()) Text("打卡后这里会长出一周的小柱子")
            }
        }
    }
}

@Composable
private fun HistoryCard(session: SleepSessionEntity) {
    Card(shape = cardShape) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(session.sessionDate, fontWeight = FontWeight.Bold)
                Text(formatDuration(session.restWindowMinutes), fontWeight = FontWeight.Bold, color = Sage)
            }
            Text("${formatTime(session.sleepCheckInAt)} → ${session.wakeCheckInAt?.let(::formatTime) ?: "未完成"}")
            if (session.usageDataAvailable == true) {
                Text("睡前 30 分钟 ${session.preSleepPhoneMinutes ?: 0} 分 · 夜间解锁 ${session.nightUnlockCount ?: 0} 次 / ${session.nightPhoneMinutes ?: 0} 分")
            } else if (session.usageDataAvailable == false) {
                Text("手机使用数据不可用", color = Blush)
            }
            if (session.tags().isNotEmpty()) Text(session.tags().joinToString(" · "), fontSize = 13.sp)
        }
    }
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
