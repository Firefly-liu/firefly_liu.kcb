package com.firefly_liu.kcb.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import com.firefly_liu.kcb.MainActivity
import com.firefly_liu.kcb.data.ScheduleRepository
import com.firefly_liu.kcb.data.db.AppDatabase
import com.firefly_liu.kcb.data.engine.ScheduleEngine
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.SemesterConfig
import com.firefly_liu.kcb.ui.weekdayShort
import java.time.LocalDate
import java.time.LocalTime

/** 小组件「看的是第几天」：0=今天，1=明天 …… 存 SharedPreferences */
object WidgetPrefs {
    const val FILE = "kcb_widget"
    const val KEY_OFFSET = "day_offset"
    val DELTA_KEY = ActionParameters.Key<Int>("delta")

    fun offset(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_OFFSET, 0)

    fun setOffset(context: Context, value: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_OFFSET, value.coerceIn(0, 14)).apply()
    }
}

/** 桌面小组件：今日 / 明日课程 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = ScheduleRepository(AppDatabase.get(context))
        val config = repo.loadConfig()
        val offset = WidgetPrefs.offset(context)
        val date = LocalDate.now().plusDays(offset.toLong())
        val items = if (config != null) {
            ScheduleEngine.instancesOf(date, repo.loadCourses(), repo.loadOverrides(), config)
        } else {
            emptyList()
        }

        provideContent {
            GlanceTheme {
                WidgetBody(context, date, offset, items, config)
            }
        }
    }
}

@Composable
private fun WidgetBody(
    context: Context,
    date: LocalDate,
    offset: Int,
    items: List<CourseInstance>,
    config: SemesterConfig?,
) {
    val label = when (offset) {
        0 -> "今天"
        1 -> "明天"
        2 -> "后天"
        else -> "${offset} 天后"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(10.dp)
            // 点组件空白处打开 App
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
    ) {
        // 标题行：◀ 今天 周六 10/10 ▶
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                "◀",
                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .padding(end = 6.dp)
                    .clickable(
                        actionRunCallback<WidgetShiftDayAction>(
                            actionParametersOf(WidgetPrefs.DELTA_KEY to -1)
                        )
                    ),
            )
            Text(
                "$label ${weekdayShort(date)} ${date.monthValue}/${date.dayOfMonth}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                "▶",
                style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier
                    .padding(start = 6.dp)
                    .clickable(
                        actionRunCallback<WidgetShiftDayAction>(
                            actionParametersOf(WidgetPrefs.DELTA_KEY to 1)
                        )
                    ),
            )
        }
        Spacer(GlanceModifier.height(6.dp))

        when {
            config == null -> Text(
                "还没设置学期，打开 App 填一下",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )

            items.isEmpty() -> Text(
                if (offset == 0) "今天没课" else "$label 没课",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )

            else -> items.take(6).forEach { inst ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    Text(
                        timeLabel(inst, config),
                        style = TextStyle(color = GlanceTheme.colors.primary),
                        modifier = GlanceModifier.width(84.dp),
                    )
                    Text(
                        buildString {
                            append(inst.name)
                            if (inst.position.isNotBlank()) append(" @").append(inst.position)
                        },
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                    )
                }
            }
        }
    }
}

/** 优先用自定义时间，否则把节次换算成时间 */
private fun timeLabel(inst: CourseInstance, config: SemesterConfig): String {
    inst.customTimeLabel?.let { return it }
    val slots = config.slots
    val s: LocalTime? = slots.firstOrNull { it.number == inst.startSection }?.start
    val e: LocalTime? = slots.firstOrNull { it.number == inst.endSection }?.end
    return if (s != null && e != null) "$s-$e" else "${inst.startSection}-${inst.endSection}节"
}


