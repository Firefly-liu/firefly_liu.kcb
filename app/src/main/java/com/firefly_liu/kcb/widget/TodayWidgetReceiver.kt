package com.firefly_liu.kcb.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetScheduler.schedule(context)
    }
}

/** 点 ◀ / ▶ 切换小组件显示的是第几天 */
class WidgetShiftDayAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val delta = parameters[WidgetPrefs.DELTA_KEY] ?: 0
        val next = WidgetPrefs.offset(context) + delta
        WidgetPrefs.setOffset(context, next)
        // 必须用 updateAll —— 实测 update(context, glanceId) 会挂住不返回，
        // 状态能写进 prefs 但界面不刷新。
        TodayWidget().updateAll(context)
    }
}

/** 小组件刷新：每天 0 点和 12 点各一次 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        TodayWidget().updateAll(applicationContext)
        return Result.success()
    }
}

object WidgetScheduler {
    private const val WORK_NAME = "kcb_widget_refresh"

    fun schedule(context: Context) {
        val now = LocalDateTime.now()
        val next = if (now.hour < 12) {
            now.toLocalDate().atTime(12, 0)
        } else {
            now.toLocalDate().plusDays(1).atTime(0, 0)
        }
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(12, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** 数据变化时立即刷新（调用方在自己的协程 scope 里调） */
    suspend fun refreshNow(context: Context) {
        TodayWidget().updateAll(context)
    }
}











