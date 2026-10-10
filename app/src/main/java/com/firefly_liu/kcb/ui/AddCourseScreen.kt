package com.firefly_liu.kcb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.firefly_liu.kcb.data.model.ClassSlot
import com.firefly_liu.kcb.data.model.Course

enum class WeekMode { ALL, ODD, EVEN, CUSTOM }

private enum class SlotPick { NONE, START, END }

/**
 * 手动添加课程。
 * 流程：填名称 → 选星期 → 选周次（每周/单周/双周/自定义）→ 选节次（附对应时间）
 */
@Composable
fun AddCourseScreen(
    totalWeeks: Int,
    slots: List<ClassSlot>,
    onCancel: () -> Unit,
    onSave: (Course) -> Unit,
) {
    val maxSection = slots.maxOfOrNull { it.number } ?: 10

    var name by remember { mutableStateOf("") }
    var teacher by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var day by remember { mutableIntStateOf(1) }
    var weekMode by remember { mutableStateOf(WeekMode.ALL) }
    var customWeeks by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var startSection by remember { mutableIntStateOf(1) }
    var endSection by remember { mutableIntStateOf(2) }
    var slotPick by remember { mutableStateOf(SlotPick.NONE) }
    var useCustomTime by remember { mutableStateOf(false) }
    var customStartText by remember { mutableStateOf("19:00") }
    var customEndText by remember { mutableStateOf("21:00") }

    // 周次模式 -> 具体周次集合
    val weeks: Set<Int> = when (weekMode) {
        WeekMode.ALL -> (1..totalWeeks).toSet()
        WeekMode.ODD -> (1..totalWeeks).filter { it % 2 == 1 }.toSet()
        WeekMode.EVEN -> (1..totalWeeks).filter { it % 2 == 0 }.toSet()
        WeekMode.CUSTOM -> customWeeks
    }

    val startTime = slots.firstOrNull { it.number == startSection }?.start
    val endTime = slots.firstOrNull { it.number == endSection }?.end

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("添加课程", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("课程名（必填）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = teacher,
            onValueChange = { teacher = it },
            label = { Text("教师（可选）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = position,
            onValueChange = { position = it },
            label = { Text("地点（可选）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))
        SectionTitle("星期")
        (1..7).chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    if (day == d) {
                        Button(onClick = { day = d }) { Text(dayName(d)) }
                    } else {
                        OutlinedButton(onClick = { day = d }) { Text(dayName(d)) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))
        SectionTitle("周次")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WeekModeChip("每周", weekMode == WeekMode.ALL) { weekMode = WeekMode.ALL }
            WeekModeChip("单周", weekMode == WeekMode.ODD) { weekMode = WeekMode.ODD }
            WeekModeChip("双周", weekMode == WeekMode.EVEN) { weekMode = WeekMode.EVEN }
            WeekModeChip("自定义", weekMode == WeekMode.CUSTOM) { weekMode = WeekMode.CUSTOM }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (weekMode == WeekMode.CUSTOM) "已选 ${weeks.size} 周（点击下方格子增减）"
            else "共 ${weeks.size} 周",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (weekMode == WeekMode.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            (1..totalWeeks).chunked(7).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { w ->
                        val on = w in customWeeks
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (on) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    customWeeks = if (on) customWeeks - w else customWeeks + w
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$w",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (on) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("上课时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            TextButton(onClick = { useCustomTime = !useCustomTime }) {
                Text(if (useCustomTime) "改回按节次" else "用任意时间")
            }
        }
        Spacer(Modifier.height(8.dp))

        if (useCustomTime) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customStartText,
                    onValueChange = { customStartText = it },
                    label = { Text("开始 HH:mm") },
                    singleLine = true,
                    isError = parseHm(customStartText) == null,
                    modifier = Modifier.weight(1f),
                )
                Text(" 到 ", modifier = Modifier.padding(horizontal = 6.dp))
                OutlinedTextField(
                    value = customEndText,
                    onValueChange = { customEndText = it },
                    label = { Text("结束 HH:mm") },
                    singleLine = true,
                    isError = parseHm(customEndText) == null,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(6.dp))
            val cs = parseHm(customStartText)
            val ce = parseHm(customEndText)
            if (cs != null && ce != null) {
                Text(
                    "将摆在第 ${sectionOf(cs, slots, maxSection)} ~ ${sectionOf(ce, slots, maxSection)} 节的高度",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "时间格式：HH:mm，例如 19:00",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { slotPick = SlotPick.START }) { Text("第 $startSection 节") }
                Text(" 到 ", modifier = Modifier.padding(horizontal = 8.dp))
                OutlinedButton(onClick = { slotPick = SlotPick.END }) { Text("第 $endSection 节") }
            }
            Spacer(Modifier.height(6.dp))
            if (startTime != null && endTime != null) {
                Text(
                    "对应时间：$startTime ~ $endTime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(
                onClick = {
                    val cs = parseHm(customStartText)
                    val ce = parseHm(customEndText)
                    if (useCustomTime && cs != null && ce != null) {
                        val a = sectionOf(cs, slots, maxSection)
                        val b = sectionOf(ce, slots, maxSection)
                        onSave(
                            Course(
                                name = name.trim(), teacher = teacher.trim(),
                                position = position.trim(), day = day,
                                startSection = minOf(a, b), endSection = maxOf(a, b),
                                weeks = weeks, source = "手动",
                                customStartMin = cs, customEndMin = ce,
                            )
                        )
                    } else {
                        onSave(
                            Course(
                                name = name.trim(), teacher = teacher.trim(),
                                position = position.trim(), day = day,
                                startSection = startSection, endSection = endSection,
                                weeks = weeks, source = "手动",
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && weeks.isNotEmpty() &&
                    if (useCustomTime) parseHm(customStartText) != null && parseHm(customEndText) != null
                    else startSection <= endSection,
                modifier = Modifier.weight(1f),
            ) { Text("保存") }
        }
        Spacer(Modifier.height(24.dp))
    }

    // 节次选择
    if (slotPick != SlotPick.NONE) {
        val isStart = slotPick == SlotPick.START
        AlertDialog(
            onDismissRequest = { slotPick = SlotPick.NONE },
            title = { Text(if (isStart) "开始节次" else "结束节次") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    slots.forEach { slot ->
                        TextButton(
                            onClick = {
                                if (isStart) {
                                    startSection = slot.number
                                    if (endSection < startSection) endSection = startSection
                                } else {
                                    endSection = slot.number
                                    if (startSection > endSection) startSection = endSection
                                }
                                slotPick = SlotPick.NONE
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("第 ${slot.number} 节") }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { slotPick = SlotPick.NONE }) { Text("取消") } },
        )
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun WeekModeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) Button(onClick = onClick) { Text(text) }
    else OutlinedButton(onClick = onClick) { Text(text) }
}

/** "HH:mm" -> 一天内分钟数 */
private fun parseHm(s: String): Int? {
    val parts = s.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/** 某个时刻落在第几节（用于把任意时间摆到网格对应高度） */
private fun sectionOf(minOfDay: Int, slots: List<ClassSlot>, fallback: Int): Int =
    slots.firstOrNull { minOfDay < it.end.toSecondOfDay() / 60 }?.number ?: fallback

private fun dayName(d: Int): String = when (d) {
    1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"
    5 -> "周五"; 6 -> "周六"; else -> "周日"
}

