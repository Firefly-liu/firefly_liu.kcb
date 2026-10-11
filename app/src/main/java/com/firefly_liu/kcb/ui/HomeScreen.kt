package com.firefly_liu.kcb.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.firefly_liu.kcb.data.BackupHelper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.firefly_liu.kcb.data.BackgroundPrefs
import com.firefly_liu.kcb.data.ScheduleRepository
import com.firefly_liu.kcb.data.engine.ScheduleEngine
import com.firefly_liu.kcb.data.model.Course
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.CourseSnapshot
import com.firefly_liu.kcb.data.model.DateRemap
import com.firefly_liu.kcb.data.model.InstanceOverride
import com.firefly_liu.kcb.data.model.Override
import com.firefly_liu.kcb.data.model.OverrideAction
import com.firefly_liu.kcb.data.model.SemesterConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** 待选目标日期的操作 */
sealed interface PendingAction {
    data class CopyDay(val source: LocalDate) : PendingAction
    data class MoveDay(val source: LocalDate) : PendingAction
    data class CopyCourse(val date: LocalDate, val inst: CourseInstance) : PendingAction
    data class MoveCourse(val date: LocalDate, val inst: CourseInstance) : PendingAction
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    config: SemesterConfig,
    repo: ScheduleRepository,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    val bgPrefs = remember { BackgroundPrefs(context) }

    val today = remember { LocalDate.now() }
    val initialWeek = remember(config) {
        ScheduleEngine.weekOf(today, config.startDate).coerceIn(1, config.totalWeeks)
    }
    var weekNo by remember { mutableIntStateOf(initialWeek) }
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var overrides by remember { mutableStateOf<List<Override>>(emptyList()) }
    var dayMenu by remember { mutableStateOf<LocalDate?>(null) }
    var courseMenu by remember { mutableStateOf<Pair<LocalDate, CourseInstance>?>(null) }
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    var pendingSlot by remember { mutableStateOf<Pair<PendingAction, LocalDate>?>(null) }
    var showBgDialog by remember { mutableStateOf(false) }
    var showAddCourse by remember { mutableStateOf(false) }
    var showAdjustments by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showTeaching by remember { mutableStateOf(false) }
    var editingSemester by remember { mutableStateOf(false) }
    var conflictPrompt by remember { mutableStateOf<ConflictPrompt?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var bgAlpha by remember { mutableFloatStateOf(bgPrefs.alpha) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            courses = repo.loadCourses()
            overrides = repo.loadOverrides()
            // 数据变了，桌面小组件也跟着刷新
            runCatching { com.firefly_liu.kcb.widget.WidgetScheduler.refreshNow(context) }
        }
    }

    // 载入数据 + 背景
    LaunchedEffect(Unit) {
        courses = repo.loadCourses()
        overrides = repo.loadOverrides()
        bgBitmap = withContext(Dispatchers.IO) { bgPrefs.loadBitmap() }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        bgPrefs.file.outputStream().use { output -> input.copyTo(output) }
                    }
                }
                bgBitmap = withContext(Dispatchers.IO) { bgPrefs.loadBitmap() }
            }
        }
    }

    val weekDates = remember(weekNo, config) {
        val monday = config.startDate.plusWeeks((weekNo - 1).toLong())
        (0..6).map { monday.plusDays(it.toLong()) }
    }

    val itemsByDate = remember(weekDates, courses, overrides, config) {
        weekDates.associateWith { d ->
            ScheduleEngine.instancesOf(d, courses, overrides, config)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("课程表", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = { showImport = true }) { Text("导入") }
                TextButton(onClick = { showAddCourse = true }) { Text("＋ 添加") }
                TextButton(onClick = { showAdjustments = true }) { Text("记录") }
                TextButton(onClick = { showSettings = true }) { Text("设置") }
            }
        }

        if (courses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("还没有课程数据", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    scope.launch {
                        repo.replaceCourses(demoCourses())
                        courses = repo.loadCourses()
                    }
                }) { Text("载入演示课表（开发用）") }
            }
        }

        // 背景 + 周视图
        Box(modifier = Modifier.weight(1f)) {
            val bmp = bgBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(bgAlpha),
                )
            }
            WeekView(
                weekNo = weekNo,
                totalWeeks = config.totalWeeks,
                weekDates = weekDates,
                itemsByDate = itemsByDate,
                slots = config.slots,
                today = today,
                onPrev = { if (weekNo > 1) weekNo-- },
                onNext = { if (weekNo < config.totalWeeks) weekNo++ },
                onLongPressDay = { dayMenu = it },
                onLongPressCourse = { d, inst -> courseMenu = d to inst },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // ---------- 背景设置 ----------
    if (showBgDialog) {
        AlertDialog(
            onDismissRequest = { showBgDialog = false },
            title = { Text("自定义背景") },
            text = {
                Column {
                    Button(
                        onClick = {
                            pickImage.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (bgPrefs.hasBackground()) "更换图片" else "选择图片") }

                    Spacer(Modifier.height(16.dp))
                    Text("背景强度：${(bgAlpha * 100).toInt()}%")
                    Slider(
                        value = bgAlpha,
                        onValueChange = {
                            bgAlpha = it
                            bgPrefs.alpha = it
                        },
                        valueRange = 0.05f..0.9f,
                    )

                    if (bgPrefs.hasBackground()) {
                        TextButton(onClick = {
                            bgPrefs.clear()
                            bgBitmap = null
                        }) { Text("清除背景") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBgDialog = false }) { Text("完成") } },
        )
    }

    // ---------- 日期长按菜单 ----------
    dayMenu?.let { day ->
        AlertDialog(
            onDismissRequest = { dayMenu = null },
            title = { Text("${day}（${weekdayShort(day)}）") },
            text = {
                Column {
                    TextButton(onClick = { pending = PendingAction.CopyDay(day); dayMenu = null }) {
                        Text("复制整天的课到…")
                    }
                    TextButton(onClick = { pending = PendingAction.MoveDay(day); dayMenu = null }) {
                        Text("把整天的课移到…")
                    }
                    TextButton(onClick = {
                        scope.launch {
                            repo.addDateRemap(DateRemap(targetDate = day, sourceWeekday = null, note = "放假"))
                            reload()
                        }
                        dayMenu = null
                    }) {
                        Text("当天放假（不上课）")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dayMenu = null }) { Text("取消") } },
        )
    }

    // ---------- 课程长按菜单 ----------
    courseMenu?.let { (date, inst) ->
        AlertDialog(
            onDismissRequest = { courseMenu = null },
            title = { Text(inst.name) },
            text = {
                Column {
                    Text("${date} ${inst.startSection}-${inst.endSection} 节", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { pending = PendingAction.CopyCourse(date, inst); courseMenu = null }) {
                        Text("复制到其他日期 / 节次…")
                    }
                    TextButton(onClick = { pending = PendingAction.MoveCourse(date, inst); courseMenu = null }) {
                        Text("移动到其他日期 / 节次…")
                    }
                    TextButton(onClick = {
                        // 复制/移动出来的课没有 sourceCourseId，靠 snapshot 一样能匹配，不能判空
                            scope.launch {
                                repo.addInstanceOverride(
                                    InstanceOverride(
                                        date = date,
                                        action = OverrideAction.REMOVE,
                                    targetCourseId = inst.sourceCourseId,
                                        snapshot = CourseSnapshot.of(inst),
                                        originLabel = "删除",
                                    )
                                )
                                reload()
                        }
                        courseMenu = null
                    }) {
                        Text("删除这节课")
                    }
                }
            },
            confirmButton = { TextButton(onClick = { courseMenu = null }) { Text("取消") } },
        )
    }

    // ---------- 目标日期选择 ----------
    pending?.let { action ->
        // 默认选中「本次操作涉及的那一天」，而不是死板的今天
        val sourceDate = when (action) {
            is PendingAction.CopyDay -> action.source
            is PendingAction.MoveDay -> action.source
            is PendingAction.CopyCourse -> action.date
            is PendingAction.MoveCourse -> action.date
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = sourceDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pending = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val target = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()

                        if (action is PendingAction.CopyCourse || action is PendingAction.MoveCourse) {
                            // 单节课操作：还需选目标节次
                            pendingSlot = action to target
                        } else {
                            val cs = conflictsAt(target, null, courses, overrides, config)
                            if (cs.isEmpty()) {
                                scope.launch {
                                    applyAction(repo, action, target, null, config.slots.size)
                                    reload()
                                }
                            } else {
                                conflictPrompt = ConflictPrompt(action, target, null, cs)
                            }
                        }
                    }
                    pending = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }

    // ---------- 导出 / 导入 ----------
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = BackupHelper.exportJson(repo)
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.onSuccess {
                    Toast.makeText(context, "已导出，文件里含课表、调休和学期设置", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "导出失败：${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val json = context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.use { it.readText() } ?: error("读不到文件内容")
                    BackupHelper.importJson(repo, json)
                }.onSuccess { n ->
                    reload()
                    Toast.makeText(context, "导入成功，共 $n 门课", Toast.LENGTH_LONG).show()
                }.onFailure {
                    Toast.makeText(context, "导入失败：${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ---------- 使用教学（全屏）----------
    if (showTeaching) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showTeaching = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            TeachingScreen(onBack = { showTeaching = false })
        }
    }

    // ---------- 学期设置（编辑，不动课表）----------
    if (editingSemester) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { editingSemester = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SetupWizardScreen(
                onSave = { cfg ->
                    scope.launch {
                        repo.saveConfig(cfg)
                        reload()
                        editingSemester = false
                        Toast.makeText(context, "学期设置已更新", Toast.LENGTH_SHORT).show()
                    }
                },
                initial = config,
                onCancel = { editingSemester = false },
            )
        }
    }

    // ---------- 设置 / 关于（全屏）----------
    if (showSettings) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSettings = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            SettingsScreen(
                versionName = "0.1.0",
                onBack = { showSettings = false },
                onOpenBackground = {
                    showSettings = false
                    showBgDialog = true
                },
                onPinWidget = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val mgr = AppWidgetManager.getInstance(context)
                        if (mgr.isRequestPinAppWidgetSupported) {
                            mgr.requestPinAppWidget(
                                ComponentName(context, com.firefly_liu.kcb.widget.TodayWidgetReceiver::class.java),
                                null,
                                null,
                            )
                        }
                    }
                },
                onOpenTeaching = {
                    showSettings = false
                    showTeaching = true
                },
                onEditSemester = {
                    showSettings = false
                    editingSemester = true
                },
                onExport = {
                    exportLauncher.launch("课程表备份-${LocalDate.now()}.json")
                },
                onImport = {
                    importLauncher.launch(arrayOf("application/json"))
                },
            )
        }
    }

    // ---------- 从教务导入（全屏 WebView）----------
    if (showImport) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImport = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ImportScreen(
                onBack = { showImport = false },
                onParsed = { list, maxWeek ->
                    scope.launch {
                        repo.replaceCourses(list)
                        // 教务给的周数比手填的更准，顺手对齐
                        if (maxWeek > 0 && maxWeek != config.totalWeeks) {
                            repo.saveConfig(config.copy(totalWeeks = maxWeek))
                        }
                        reload()
                        showImport = false
                    }
                },
            )
        }
    }

    // ---------- 冲突提示 ----------
    conflictPrompt?.let { cp ->
        AlertDialog(
            onDismissRequest = { conflictPrompt = null },
            title = { Text("这个时段已经有课") },
            text = {
                Column {
                    Text("${cp.target} 该位置已有 ${cp.conflicts.size} 门课：")
                    Spacer(Modifier.height(6.dp))
                    cp.conflicts.forEach { n ->
                        Text("· $n", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "仍要放进去的话，两门课会在同一格里并排显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        applyAction(repo, cp.action, cp.target, cp.startSection, config.slots.size)
                        reload()
                    }
                    conflictPrompt = null
                }) { Text("仍然放置") }
            },
            dismissButton = { TextButton(onClick = { conflictPrompt = null }) { Text("取消") } },
        )
    }

    // ---------- 调整记录（可逐条撤销）----------
    if (showAdjustments) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAdjustments = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(20.dp),
            ) {
                Text("调整记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "共 ${overrides.size} 条。删除任意一条即可撤销那次调整。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                if (overrides.isEmpty()) {
                    Text("还没有任何调整", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        overrides.forEach { ov ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    describeOverride(ov),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    scope.launch {
                                        repo.removeOverride(ov)
                                        reload()
                                    }
                                }) { Text("撤销") }
                            }
                            HorizontalDivider()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showAdjustments = false },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("关闭") }
            }
        }
    }

    // ---------- 手动添加课程（全屏）----------
    if (showAddCourse) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddCourse = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            AddCourseScreen(
                totalWeeks = config.totalWeeks,
                slots = config.slots,
                onCancel = { showAddCourse = false },
                onSave = { course ->
                    scope.launch {
                        repo.addCourse(course)
                        showAddCourse = false
                        reload()
                    }
                },
            )
        }
    }

    // ---------- 目标节次选择（仅单节课操作）----------
    pendingSlot?.let { (action, date) ->
        AlertDialog(
            onDismissRequest = { pendingSlot = null },
            title = { Text("放在第几节？") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    config.slots.forEach { slot ->
                        TextButton(
                            onClick = {
                                val range = targetRange(action, slot.number, config.slots.size)
                                val cs = conflictsAt(date, range, courses, overrides, config)
                                if (cs.isEmpty()) {
                                    scope.launch {
                                        applyAction(repo, action, date, slot.number, config.slots.size)
                                        reload()
                                    }
                                } else {
                                    conflictPrompt = ConflictPrompt(action, date, slot.number, cs)
                                }
                                pendingSlot = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("第 ${slot.number} 节") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pendingSlot = null }) { Text("取消") } },
        )
    }
}

private suspend fun applyAction(
    repo: ScheduleRepository,
    action: PendingAction,
    target: LocalDate,
    newStartSection: Int? = null,
    totalSlots: Int = 10,
) {
    when (action) {
        is PendingAction.CopyDay ->
            repo.addDateRemap(
                DateRemap(targetDate = target, sourceWeekday = action.source.dayOfWeek.value, note = "复制自 ${action.source}")
            )

        is PendingAction.MoveDay -> {
            repo.addDateRemap(
                DateRemap(targetDate = target, sourceWeekday = action.source.dayOfWeek.value, note = "移动自 ${action.source}")
            )
            repo.addDateRemap(
                DateRemap(targetDate = action.source, sourceWeekday = null, note = "移出到 $target")
            )
        }

        is PendingAction.CopyCourse ->
            repo.addInstanceOverride(
                InstanceOverride(
                    date = target,
                    action = OverrideAction.ADD,
                    snapshot = withSections(CourseSnapshot.of(action.inst), newStartSection, totalSlots),
                    originDate = action.date,
                    originLabel = "复制自 ${action.date}",
                )
            )

        is PendingAction.MoveCourse -> {
            repo.addInstanceOverride(
                InstanceOverride(
                    date = target,
                    action = OverrideAction.ADD,
                    snapshot = withSections(CourseSnapshot.of(action.inst), newStartSection, totalSlots),
                    originDate = action.date,
                    originLabel = "移动自 ${action.date}",
                )
            )
            action.inst.sourceCourseId?.let { cid ->
                repo.addInstanceOverride(
                    InstanceOverride(
                        date = action.date,
                        action = OverrideAction.REMOVE,
                        targetCourseId = cid,
                        snapshot = CourseSnapshot.of(action.inst),
                        originLabel = "移动到 $target",
                    )
                )
            }
        }
    }
}

private data class ConflictPrompt(
    val action: PendingAction,
    val target: LocalDate,
    val startSection: Int?,
    val conflicts: List<String>,
)

/** 算出这次操作会落到哪个节次区间 */
private fun targetRange(action: PendingAction, startSection: Int?, totalSlots: Int): IntRange {
    val inst = when (action) {
        is PendingAction.CopyCourse -> action.inst
        is PendingAction.MoveCourse -> action.inst
        else -> return 0..0
    }
    if (startSection == null) return inst.startSection..inst.endSection
    val span = inst.endSection - inst.startSection
    return startSection..minOf(startSection + span, totalSlots)
}

/** 目标位置已有的课程名（range 为 null 表示整天） */
private fun conflictsAt(
    date: LocalDate,
    range: IntRange?,
    courses: List<Course>,
    overrides: List<Override>,
    config: SemesterConfig,
): List<String> {
    val existing = ScheduleEngine.instancesOf(date, courses, overrides, config)
    return if (range == null) existing.map { it.name }
    else existing.filter { it.startSection <= range.last && range.first <= it.endSection }.map { it.name }
}

/** 把一条覆盖记录描述成人话 */
private fun describeOverride(o: Override): String = when (o) {
    is DateRemap ->
        if (o.sourceWeekday == null) "${o.targetDate} 放假"
        else "${o.targetDate} 按${weekdayShortOf(o.sourceWeekday)}的课表上课${if (o.note.isNotBlank()) "（${o.note}）" else ""}"
    is InstanceOverride -> {
        val who = o.snapshot?.name ?: "课程"
        val what = if (o.action == OverrideAction.ADD) "添加" else "移除"
        "${o.date} $what「$who」${if (o.originLabel.isNotBlank()) "（${o.originLabel}）" else ""}"
    }
}

private fun weekdayShortOf(d: Int): String = when (d) {
    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
    5 -> "周五"; 6 -> "周六"; else -> "周日"
}

/** 按目标起始节调整快照的节次范围，保持原有跨度 */
private fun withSections(s: CourseSnapshot, newStart: Int?, totalSlots: Int): CourseSnapshot {
    if (newStart == null) return s
    val span = s.endSection - s.startSection
    val end = minOf(newStart + span, totalSlots)
    return s.copy(startSection = newStart, endSection = end)
}

/** 开发期用的示例课表（虚构数据，仅用于验证渲染） */
fun demoCourses(): List<Course> = listOf(
    Course(id = 1, name = "高等数学", teacher = "李老师", position = "教学楼A101", day = 1, startSection = 1, endSection = 2, weeks = (1..16).toSet()),
    Course(id = 2, name = "大学英语", teacher = "王老师", position = "教学楼B203", day = 1, startSection = 3, endSection = 4, weeks = (1..16).toSet()),
    Course(id = 3, name = "线性代数", teacher = "张老师", position = "教学楼A102", day = 2, startSection = 5, endSection = 6, weeks = (1..8).toSet()),
    Course(id = 4, name = "大学物理", teacher = "刘老师", position = "实验楼C301", day = 2, startSection = 5, endSection = 6, weeks = (9..16).toSet()),
    Course(id = 5, name = "程序设计基础", teacher = "陈老师", position = "机房D401", day = 3, startSection = 1, endSection = 2, weeks = (1..16).toSet()),
    Course(id = 6, name = "中国近现代史纲要", teacher = "赵老师", position = "教学楼A205", day = 3, startSection = 5, endSection = 6, weeks = (9..16).toSet()),
    Course(id = 7, name = "体育", teacher = "孙老师", position = "体育馆", day = 4, startSection = 5, endSection = 6, weeks = (1..16).toSet()),
    Course(id = 8, name = "数字电路", teacher = "周老师", position = "实验楼C202", day = 5, startSection = 1, endSection = 2, weeks = (1..16).toSet()),
    Course(id = 9, name = "离散数学", teacher = "吴老师", position = "教学楼B105", day = 5, startSection = 3, endSection = 4, weeks = (1..8).toSet()),
    Course(id = 10, name = "工程制图", teacher = "郑老师", position = "教学楼A110", day = 5, startSection = 5, endSection = 6, weeks = (5..12).toSet()),
)


















