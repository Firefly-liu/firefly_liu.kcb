package com.firefly_liu.kcb.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.firefly_liu.kcb.data.model.Course
import org.json.JSONObject

/** 学校 WebVPN 入口；用户可以自己在页面里导航 */
private const val START_URL = "https://webvpn.hljut.edu.cn/"

/** 教务系统对移动端不友好，伪装成桌面浏览器 */
private const val DESKTOP_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/** 注入给页面的桥：适配脚本通过它回传数据 */
private class KcbBridge(
    private val onMessage: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onCourses: (String) -> Unit,
    private val onDone: () -> Unit,
) {
    @JavascriptInterface fun showToast(msg: String) = onMessage(msg)
    @JavascriptInterface fun reportError(msg: String) = onError(msg)
    @JavascriptInterface fun saveCourses(json: String) = onCourses(json)
    @JavascriptInterface fun notifyTaskCompletion() = onDone()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onParsed: (List<Course>, Int) -> Unit,
) {
    val context = LocalContext.current
    val main = remember { Handler(Looper.getMainLooper()) }

    var status by remember {
        mutableStateOf("① 在下面登录学校教务　② 进入「个人课表查询」　③ 等课表显示出来点「开始解析」")
    }
    var running by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.userAgentString = DESKTOP_UA
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            webViewClient = WebViewClient()
            addJavascriptInterface(
                KcbBridge(
                    onMessage = { m -> main.post { status = m } },
                    onError = { m -> main.post { status = "✗ $m"; running = false } },
                    onCourses = { json ->
                        main.post {
                            running = false
                            parseCourses(json, onParsed) { status = it }
                        }
                    },
                    onDone = { main.post { running = false } },
                ),
                "kcbBridge",
            )
            loadUrl(START_URL)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text("从教务导入", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            status,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        }
        Button(
            onClick = {
                running = true
                status = "正在解析…"
                val js = context.assets.open("adapters/hljut.js")
                    .bufferedReader().use { it.readText() }
                webView.evaluateJavascript(js, null)
            },
            enabled = !running,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) { Text(if (running) "解析中…" else "开始解析") }
    }
}

/** 把脚本回传的 JSON 转成 Course 列表 */
private fun parseCourses(
    json: String,
    onParsed: (List<Course>, Int) -> Unit,
    setStatus: (String) -> Unit,
) {
    try {
        val o = JSONObject(json)
        val arr = o.optJSONArray("courses")
        if (arr == null || arr.length() == 0) {
            setStatus("✗ 没有解析到课程")
            return
        }
        val maxWeek = o.optInt("maxWeek", 16)
        val semester = o.optString("semester", "")
        val list = mutableListOf<Course>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val weeks = mutableSetOf<Int>()
            c.optJSONArray("weeks")?.let { wa ->
                for (j in 0 until wa.length()) weeks.add(wa.getInt(j))
            }
            val name = c.optString("name")
            if (name.isBlank()) continue
            list.add(
                Course(
                    name = name,
                    teacher = c.optString("teacher"),
                    position = c.optString("position"),
                    campus = c.optString("campus"),
                    day = c.optInt("day"),
                    startSection = c.optInt("startSection"),
                    endSection = c.optInt("endSection"),
                    weeks = weeks,
                    credit = c.optDouble("credit", 0.0),
                    nature = c.optString("nature"),
                    assessment = c.optString("assessment"),
                    source = if (semester.isBlank()) "正方" else "正方/$semester",
                )
            )
        }
        if (list.isEmpty()) {
            setStatus("✗ 解析结果为空")
            return
        }
        setStatus("✓ 解析到 ${list.size} 条课程，正在保存…")
        onParsed(list, maxWeek)
    } catch (e: Exception) {
        setStatus("✗ 数据转换失败：${e.message}")
    }
}

