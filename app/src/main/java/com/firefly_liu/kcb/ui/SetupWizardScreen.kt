package com.firefly_liu.kcb.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.firefly_liu.kcb.data.model.ClassSlot
import com.firefly_liu.kcb.data.model.SemesterConfig
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * 首启向导：填开学日期 + 确认作息时间。
 * 抓取拿不到这两样，只能手动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    onSave: (SemesterConfig) -> Unit,
    initial: SemesterConfig? = null,
    onCancel: (() -> Unit)? = null,
) {
    var startDate by remember { mutableStateOf(initial?.startDate ?: mondayOf(LocalDate.now())) }
    var totalWeeks by remember { mutableIntStateOf(initial?.totalWeeks ?: 16) }
    var slots by remember { mutableStateOf(initial?.slots ?: SemesterConfig.defaultSlots()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 全屏 Dialog 里必须有不透明底色，否则会透出底层界面、文字叠在一起
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("学期设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            if (initial == null) "开学日期和作息时间抓不到，需要你填一次"
            else "改完直接生效，不会动已经导入的课表",
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(Modifier.height(24.dp))

        // --- 开学日期 ---
        Text("开学日期（第 1 周周一）", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Text(startDate.toString())
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "自动对齐到所选日期所在周的周一",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        // --- 总周数 ---
        Text("总周数", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (totalWeeks > 1) totalWeeks-- }) { Text("-") }
            Text(
                totalWeeks.toString(),
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedButton(onClick = { if (totalWeeks < 30) totalWeeks++ }) { Text("+") }
        }

        Spacer(Modifier.height(20.dp))

        // --- 作息时间 ---
        Text("作息时间（点击可改）", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                slots.forEachIndexed { index, slot ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("第 ${slot.number} 节")
                        TextButton(onClick = { editingIndex = index }) {
                            Text("${fmt(slot.start)} - ${fmt(slot.end)}")
                        }
                    }
                    if (index != slots.lastIndex) HorizontalDivider()
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = { onSave(SemesterConfig(startDate, totalWeeks, slots)) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存并开始使用")
        }

        Spacer(Modifier.height(24.dp))
    }

    // --- 日期选择 ---
    if (showDatePicker) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = startDate
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        // 自动对齐到该日期所在周的周一（引擎以周一为第 1 周起点）
                        startDate = picked.minusDays((picked.dayOfWeek.value - 1).toLong())
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            androidx.compose.material3.DatePicker(state = state)
        }
    }

    // --- 节次时间编辑 ---
    if (editingIndex >= 0) {
        val slot = slots[editingIndex]
        var startText by remember(editingIndex) { mutableStateOf(fmt(slot.start)) }
        var endText by remember(editingIndex) { mutableStateOf(fmt(slot.end)) }
        val startOk = parseTime(startText) != null
        val endOk = parseTime(endText) != null

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingIndex = -1 },
            title = { Text("第 ${slot.number} 节") },
            text = {
                Column {
                    OutlinedTextField(
                        value = startText,
                        onValueChange = { startText = it },
                        label = { Text("开始（HH:mm）") },
                        isError = !startOk,
                        singleLine = true,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = endText,
                        onValueChange = { endText = it },
                        label = { Text("结束（HH:mm）") },
                        isError = !endOk,
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = startOk && endOk,
                    onClick = {
                        val s = parseTime(startText)!!
                        val e = parseTime(endText)!!
                        slots = slots.toMutableList().also {
                            it[editingIndex] = ClassSlot(slot.number, s, e)
                        }
                        editingIndex = -1
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = -1 }) { Text("取消") }
            },
        )
    }
}

private fun mondayOf(d: LocalDate): LocalDate =
    d.minusDays((d.dayOfWeek.value - 1).toLong())

private fun fmt(t: LocalTime): String = "%02d:%02d".format(t.hour, t.minute)

private fun parseTime(s: String): LocalTime? = try {
    LocalTime.parse(s.trim())
} catch (_: Exception) {
    null
}




