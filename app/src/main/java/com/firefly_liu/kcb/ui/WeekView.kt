package com.firefly_liu.kcb.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.firefly_liu.kcb.data.model.ClassSlot
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.InstanceOrigin
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs

private val PALETTE = listOf(
    Color(0xFF5C6BC0), Color(0xFF26A69A), Color(0xFFEF5350), Color(0xFFFFA726),
    Color(0xFF66BB6A), Color(0xFFAB47BC), Color(0xFF42A5F5), Color(0xFFEC407A),
)

/** 同一门课永远同色 */
fun colorForCourse(name: String): Color = PALETTE[abs(name.hashCode()) % PALETTE.size]

fun weekdayShort(d: LocalDate): String = when (d.dayOfWeek) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}

val ROW_HEIGHT = 58.dp
private val HEAD_HEIGHT = 52.dp
private val SLOT_COL_WIDTH = 40.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeekView(
    weekNo: Int,
    totalWeeks: Int,
    weekDates: List<LocalDate>,
    itemsByDate: Map<LocalDate, List<CourseInstance>>,
    slots: List<ClassSlot>,
    today: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLongPressDay: (LocalDate) -> Unit,
    onLongPressCourse: (LocalDate, CourseInstance) -> Unit,
    onTapCourse: (LocalDate, CourseInstance) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // ---------- 周导航 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onPrev, enabled = weekNo > 1) { Text("◀") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("第 $weekNo 周", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${weekDates.first()} ~ ${weekDates.last()}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(onClick = onNext, enabled = weekNo < totalWeeks) { Text("▶") }
        }

        // ---------- 表头（长按选日期） ----------
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(SLOT_COL_WIDTH).height(HEAD_HEIGHT))
            weekDates.forEach { d ->
                val isToday = d == today
                val isPast = d < today
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(HEAD_HEIGHT)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { onLongPressDay(d) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        weekdayShort(d),
                        style = MaterialTheme.typography.labelMedium,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.primary
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        "${d.monthValue}/${d.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            isToday -> MaterialTheme.colorScheme.primary
                            isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        // ---------- 网格 ----------
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val colWidth = (maxWidth - SLOT_COL_WIDTH) / 7
                val totalHeight = ROW_HEIGHT * slots.size

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(totalHeight),
                ) {
                    weekDates.forEachIndexed { index, d ->
                        Box(
                            modifier = Modifier
                                .offset(x = SLOT_COL_WIDTH + colWidth * index)
                                .width(colWidth)
                                .height(totalHeight)
                                .background(
                                    when {
                                        d == today -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                        d < today -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                        else -> Color.Transparent
                                    }
                                )
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = { onLongPressDay(d) },
                                ),
                        )
                    }

                    Column(modifier = Modifier.width(SLOT_COL_WIDTH)) {
                        slots.forEach { slot ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(ROW_HEIGHT),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${slot.number}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    itemsByDate.forEach { (date, list) ->
                        val dayIndex = weekDates.indexOf(date)
                        if (dayIndex < 0) return@forEach
                        // 同一时段有多门课时横向并排（顺带把冲突暴露出来）
                        val placed = assignLanes(list)
                        placed.forEach { (inst, lane, lanes) ->
                            val laneWidth = colWidth / lanes
                            val top = ROW_HEIGHT * (inst.startSection - 1)
                            val h = ROW_HEIGHT * (inst.endSection - inst.startSection + 1)
                            CourseBlock(
                                inst = inst,
                                dimmed = date < today,
                                modifier = Modifier
                                    .offset(
                                        x = SLOT_COL_WIDTH + colWidth * dayIndex + laneWidth * lane,
                                        y = top,
                                    )
                                    .width(laneWidth)
                                    .height(h)
                                    .combinedClickable(
                                        onClick = { onTapCourse(date, inst) },
                                        onLongClick = { onLongPressCourse(date, inst) },
                                    ),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * 把同一天课程按时间切成「重叠簇」：只有真正互相重叠的课程才横向并排，
 * 不重叠的课程各自占满整列。返回 (课程, lane, 该簇的 lane 总数)。
 */
private fun assignLanes(list: List<CourseInstance>): List<Triple<CourseInstance, Int, Int>> {
    val result = mutableListOf<Triple<CourseInstance, Int, Int>>()
    var cluster = mutableListOf<Pair<CourseInstance, Int>>()
    var clusterEnd = -1

    fun flush() {
        if (cluster.isNotEmpty()) {
            val n = (cluster.maxOfOrNull { it.second } ?: 0) + 1
            cluster.forEach { (inst, lane) -> result.add(Triple(inst, lane, n)) }
            cluster = mutableListOf()
        }
    }

    list.sortedBy { it.startSection }.forEach { inst ->
        if (cluster.isNotEmpty() && inst.startSection > clusterEnd) {
            flush()
            clusterEnd = -1
        }
        var lane = 0
        while (cluster.any { (o, l) ->
                l == lane && o.startSection <= inst.endSection && inst.startSection <= o.endSection
            }
        ) lane++
        cluster.add(inst to lane)
        clusterEnd = maxOf(clusterEnd, inst.endSection)
    }
    flush()
    return result
}

@Composable
private fun CourseBlock(
    inst: CourseInstance,
    dimmed: Boolean,
    modifier: Modifier,
) {
    val base = colorForCourse(inst.name)
    Box(
        modifier = modifier
            .padding(1.5.dp)
            .alpha(if (dimmed) 0.55f else 1f)
            .clip(RoundedCornerShape(7.dp))
            .background(base)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Column {
            Text(
                inst.name,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            inst.customTimeLabel?.let { t ->
                Text(
                    t,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (inst.position.isNotBlank()) {
                Text(
                    "@${inst.position}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (inst.origin == InstanceOrigin.REMAPPED) {
                Text(
                    "调休",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            } else if (inst.origin == InstanceOrigin.OVERRIDE) {
                Text(
                    "已调",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}




