package com.firefly_liu.kcb.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import com.firefly_liu.kcb.data.ScheduleRepository
import com.firefly_liu.kcb.data.db.AppDatabase
import com.firefly_liu.kcb.data.engine.ScheduleEngine
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.SemesterConfig
import com.firefly_liu.kcb.ui.weekdayShort
import java.time.LocalDate

/** 桌面小组件：今日课程 */
class TodayWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        android.util.Log.d("KCBW", "provideGlance start")
        val repo = ScheduleRepository(AppDatabase.get(context))
        val config = repo.loadConfig()
        android.util.Log.d("KCBW", "config loaded: " + (config != null))
        val today = LocalDate.now()
        val items = if (config != null) {
            ScheduleEngine.instancesOf(
                today, repo.loadCourses(), repo.loadOverrides(), config,
            )
        } else {
            emptyList()
        }

        android.util.Log.d("KCBW", "items=" + items.size)
        provideContent {
            android.util.Log.d("KCBW", "provideContent composing")
            GlanceTheme {
                WidgetBody(today, items, config)
            }
        }
        android.util.Log.d("KCBW", "provideGlance end")
    }
}

@Composable
private fun WidgetBody(
    today: LocalDate,
    items: List<CourseInstance>,
    config: SemesterConfig?,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .padding(12.dp),
    ) {
        Text(
            "今天 ${weekdayShort(today)} ${today.monthValue}/${today.dayOfMonth}",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(6.dp))

        when {
            config == null -> Text(
                "还没设置学期，打开 App 填一下",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
            )

            items.isEmpty() -> Text(
                "今天没课",
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
                        inst.customTimeLabel ?: "${inst.startSection}-${inst.endSection}节",
                        style = TextStyle(color = GlanceTheme.colors.primary),
                        modifier = GlanceModifier.width(56.dp),
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


