package com.firefly_liu.kcb.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 设置页（含关于信息） */
@Composable
fun SettingsScreen(
    versionName: String,
    onBack: () -> Unit,
    onOpenBackground: () -> Unit,
    onPinWidget: () -> Unit,
    onOpenTeaching: () -> Unit,
    onEditSemester: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    // 本地专用声明：不在仓库里的 local_only.xml 里，没有就整段不显示
    val context = LocalContext.current
    val localNotice = remember {
        val resId = context.resources.getIdentifier("local_notice", "string", context.packageName)
        if (resId != 0) context.getString(resId) else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text("设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // ---------- 关于 ----------
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("课程表", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("版本 $versionName", style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(16.dp))
                Text("本软件由", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "24电协小刘  与  大肥鱼（DeepSeek）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text("制作", style = MaterialTheme.typography.bodyMedium)

                Spacer(Modifier.height(16.dp))
                Text(
                    "纯本地运行，不上传任何数据，也没有广告。\n" +
                        "除了登录学校教务系统之外，不会访问网络。\n" +
                        "课表、调休、背景图全部只存在你自己的手机上。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (localNotice != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        localNotice,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "2026 · 开源 · 仅供学习交流",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ---------- 个性化 ----------
        Text("个性化", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        TextButton(onClick = onOpenBackground, modifier = Modifier.fillMaxWidth()) {
            Text("自定义背景", modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onPinWidget, modifier = Modifier.fillMaxWidth()) {
            Text("把「今日课程」加到桌面", modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(24.dp))

        // ---------- 学期 ----------
        Text("学期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        TextButton(onClick = onEditSemester, modifier = Modifier.fillMaxWidth()) {
            Text("学期设置（开学日期 / 作息时间）", modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(24.dp))

        // ---------- 备份 ----------
        Text("备份与恢复", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        TextButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
            Text("导出课表（含调休记录与学期设置）", modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("导入课表（从备份文件恢复）", modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(24.dp))

        // ---------- 帮助 ----------
        Text("帮助", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        TextButton(onClick = onOpenTeaching, modifier = Modifier.fillMaxWidth()) {
            Text("使用教学", modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("完成") }
        Spacer(Modifier.height(24.dp))
    }
}


