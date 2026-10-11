package com.firefly_liu.kcb.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.firefly_liu.kcb.MainActivity
import com.firefly_liu.kcb.R
import com.firefly_liu.kcb.data.ScheduleRepository
import com.firefly_liu.kcb.data.db.AppDatabase
import com.firefly_liu.kcb.data.engine.ScheduleEngine
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.SemesterConfig
import com.firefly_liu.kcb.ui.weekdayShort
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalTime

/** 小组件「看的是第几天」：0=今天，1=明天 …… */
object WidgetPrefs {
    const val FILE = "kcb_widget"
    const val KEY_OFFSET = "day_offset"

    fun offset(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_OFFSET, 0)

    fun setOffset(context: Context, value: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_OFFSET, value.coerceIn(0, 14)).apply()
    }
}

/**
 * 今日课程小组件。
 *
 * 用传统 RemoteViews 而不是 Glance：点箭头时只 setTextViewText 改几行文字，
 * 不重建视图树、不走 Glance 的 SessionWorker，点击到刷新是毫秒级。
 */
class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SHIFT) {
            val delta = intent.getIntExtra(EXTRA_DELTA, 0)
            WidgetPrefs.setOffset(context, WidgetPrefs.offset(context) + delta)
            refreshAll(context)
        }
    }

    companion object {
        const val ACTION_SHIFT = "com.firefly_liu.kcb.WIDGET_SHIFT"
        const val EXTRA_DELTA = "delta"
        private const val MAX_ROWS = 5

        private val TIME_IDS = intArrayOf(R.id.t1_time, R.id.t2_time, R.id.t3_time, R.id.t4_time, R.id.t5_time)
        private val NAME_IDS = intArrayOf(R.id.t1_name, R.id.t2_name, R.id.t3_name, R.id.t4_name, R.id.t5_name)
        private val ROW_IDS = intArrayOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4, R.id.row5)

        /** 重画所有小组件实例 */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TodayWidgetProvider::class.java))
            ids.forEach { render(context, manager, it) }
        }

        private fun render(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_today)

            // 数据（本地 SQLite + 纯函数计算，通常几毫秒）。
            // 这里刻意用 runBlocking 同步读完再渲染：省掉协程调度与异步等待，
            // 点击到刷新之间不再有额外的一次往返。
            val offset = WidgetPrefs.offset(context)
            val date = LocalDate.now().plusDays(offset.toLong())
            val repo = ScheduleRepository(AppDatabase.get(context))
            val (config, items) = runBlocking {
                val cfg = repo.loadConfig()
                val list = if (cfg != null) {
                    ScheduleEngine.instancesOf(date, repo.loadCourses(), repo.loadOverrides(), cfg)
                } else {
                    emptyList()
                }
                cfg to list
            }

            // 标题
            val label = when (offset) {
                0 -> "今天"
                1 -> "明天"
                2 -> "后天"
                else -> "$offset 天后"
            }
            views.setTextViewText(R.id.tv_title, "$label ${weekdayShort(date)} ${date.monthValue}/${date.dayOfMonth}")

            // 左右箭头：各带一个 delta
            views.setOnClickPendingIntent(R.id.btn_prev, shiftIntent(context, -1))
            views.setOnClickPendingIntent(R.id.btn_next, shiftIntent(context, 1))

            // 整块区域点击打开 App
            views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))

            // 课程行：固定 5 行，只改文字与可见性 —— 这是速度的关键
            if (config == null || items.isEmpty()) {
                views.setViewVisibility(R.id.tv_empty, View.VISIBLE)
                views.setTextViewText(
                    R.id.tv_empty,
                    when {
                        config == null -> "还没设置学期，打开 App 填一下"
                        offset == 0 -> "今天没课"
                        else -> "$label 没课"
                    },
                )
                ROW_IDS.forEach { views.setViewVisibility(it, View.GONE) }
            } else {
                views.setViewVisibility(R.id.tv_empty, View.GONE)
                for (i in 0 until MAX_ROWS) {
                    val inst = items.getOrNull(i)
                    if (inst == null) {
                        views.setViewVisibility(ROW_IDS[i], View.GONE)
                    } else {
                        views.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                        views.setTextViewText(TIME_IDS[i], timeLabel(inst, config))
                        views.setTextViewText(
                            NAME_IDS[i],
                            buildString {
                                append(inst.name)
                                if (inst.position.isNotBlank()) append(" @").append(inst.position)
                            },
                        )
                    }
                }
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun shiftIntent(context: Context, delta: Int): PendingIntent {
            val intent = Intent(context, TodayWidgetProvider::class.java).apply {
                action = ACTION_SHIFT
                putExtra(EXTRA_DELTA, delta)
            }
            // requestCode 用 delta 区分两个箭头，否则第二个会覆盖第一个
            return PendingIntent.getBroadcast(
                context,
                if (delta < 0) 1 else 2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        /** 优先用自定义时间，否则把节次换算成时间 */
        private fun timeLabel(inst: CourseInstance, config: SemesterConfig): String {
            inst.customTimeLabel?.let { return it }
            val slots = config.slots
            val s: LocalTime? = slots.firstOrNull { it.number == inst.startSection }?.start
            val e: LocalTime? = slots.firstOrNull { it.number == inst.endSection }?.end
            return if (s != null && e != null) "$s-$e" else "${inst.startSection}-${inst.endSection}节"
        }
    }
}

