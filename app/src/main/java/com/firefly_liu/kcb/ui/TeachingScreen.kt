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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 使用教学 */
@Composable
fun TeachingScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text("使用教学", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Lesson(
            "① 先把课表导进来",
            "点顶栏「导入」→ 在输入框里填你学校的 WebVPN 地址（填一次就记住了）→ 点「前往」→ " +
                "在里面登录、进到「个人课表查询」页面 → 等课表显示出来，点右下角那个圆形「解析」按钮。\n\n" +
                "登录用你自己的账号密码，App 不会保存、也不会替你登录。",
        )

        Lesson(
            "② 设置学期",
            "开学日期和作息时间教务页面上没有，要你自己填一次。\n\n" +
                "开学日期填「第 1 周周一」，随便选一天也行，会自动对齐到那一周的周一。\n" +
                "作息时间如果和默认的不一样，点每一行就能改。\n\n" +
                "以后想改：设置 → 学期设置。改完直接生效，不会动已导入的课表。",
        )

        Lesson(
            "③ 看课表",
            "顶部「◀ 第 N 周 ▶」左右切换周次，也可以直接在课表上左右滑。\n\n" +
                "今天所在的那一列会高亮；已经过去的日期会变灰。\n" +
                "同一时间有两门课时，会自动并排显示。",
        )

        Lesson(
            "④ 调休、调课（最常用）",
            "长按日期那一列的空白处：可以「复制整天的课到某天」「整天移动到某天」「当天放假」。\n\n" +
                "长按某节课的卡片：可以复制、移动、删除这节课，还能指定放到第几节。\n\n" +
                "调休后被调的那天，课程上会显示「调休」角标；手动加过的课显示「已调」。\n\n" +
                "所有调整都是「追加记录」，随时可以在顶栏「记录」里逐条撤销，原始课表永远不动。",
        )

        Lesson(
            "⑤ 加一门课",
            "点顶栏「添加」：先填课程名，选星期，再选周次（每周 / 单周 / 双周 / 自己点周次格子），" +
                "最后选节次。\n\n" +
                "如果这门课不在标准节次里（比如晚上的选修），点右上角「用任意时间」，直接输入 " +
                "19:00 这种时间就行。",
        )

        Lesson(
            "⑥ 桌面小组件",
            "设置 → 「把『今日课程』加到桌面」→ 系统会弹确认框 → 点「Add to home screen」。\n\n" +
                "小组件显示今天要上的课，每天 0 点和 12 点自动刷新，App 里改了课也会立刻更新。\n" +
                "点小组件就能打开 App。",
        )

        Lesson(
            "⑦ 备份与恢复",
            "设置 → 「导出课表」会把课表、调休记录、学期设置一起打包成一个文件，你可以存到手机或发给自己。\n\n" +
                "换手机、重装 App 之后，用「导入课表」选那个文件就能全部恢复。",
            last = true,
        )

        Spacer(Modifier.height(24.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("知道了") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Lesson(title: String, body: String, last: Boolean = false) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (!last) {
        Spacer(Modifier.height(20.dp))
    }
}

