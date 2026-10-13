# 课程表 · firefly_liu.kcb

> 一款**纯本地、无广告、不联网**的安卓课程表 App。
>
> 本软件由 **24电协小刘** 与 **大肥鱼（DeepSeek）** 制作。

不注册、不上传、不埋点，课表只待在你自己的手机里。

---

## 特性

**导入**

- **一键导入课表**：内置 WebView 打开学校 WebVPN，你手动登录后点「解析」，由 JS 适配脚本把课表抓下来。**不保存账号密码、不代登录**
- **手动添加课程**：支持 每周 / 单周 / 双周 / 自定义周次；时间可以用节次，也可以直接输入任意时间

**查看**

- **周视图**：左右滑动切换周次，本周整列高亮，非本周低透明度
- **自定义背景**：从相册选图当课表背景，强度可调
- **桌面小组件**：显示今天 / 明天 / 后天的课，箭头一键切换；每天 0 点 / 12 点自动刷新，数据一变立刻更新

**调休调课**

- **长按即调**：长按日期或课程卡片，直接复制整天 / 移动整天 / 当天放假 / 移动单节课
  - 可以指定目标日期**和目标节次**，节次跨度自动保持
  - 所有调整都是「追加记录」，可在「记录」里逐条撤销
  - 有冲突时会提示，并排显示同一格里的多门课

**其他**

- **导入导出备份**：课表、调休、学期配置导出为一份 JSON，换机或重装后可整份导回
- **使用教学**：App 内自带上手步骤
- **学期设置可随时改**：改开学日期与作息，不影响已导入的课表
- **完全离线**：除登录学校教务外不访问网络；不申请任何多余权限

---

## 架构总览

👉 **[打开交互式架构图](docs/architecture.html)** —— 矢量图，可缩放、可点选节点查看详情。

> GitHub 不能直接内嵌 HTML，点开链接即可，或在本地浏览器打开 `docs/architecture.html`。

---

## 技术栈

| 用途 | 选型 |
| --- | --- |
| 语言 / UI | Kotlin · Jetpack Compose (Material 3) |
| 本地存储 | Room |
| 桌面小组件 | **传统 AppWidgetProvider + RemoteViews** |
| 定时刷新 | WorkManager |
| 课表抓取 | WebView + JavaScript Bridge + 适配脚本 |
| 计算引擎 | 纯函数 + JUnit 单测 |

> 小组件最初用 Jetpack Glance 实现，但点箭头时会有明显卡顿。后来**换回传统的 RemoteViews**：刷新时只 `setTextViewText` 改几行文字，不重建视图树，点击到更新是毫秒级。顺带把 `androidx.glance` 依赖去掉，APK 从 9.17 MB 降到 **7.76 MB**。

---

## 项目结构

```
app/src/main/
├── java/com/firefly_liu/kcb/
│   ├── MainActivity.kt              路由：首启向导 / 主页
│   ├── data/
│   │   ├── ScheduleRepository.kt    Room 与计算引擎的粘合层
│   │   ├── BackupHelper.kt          导入导出 JSON 备份
│   │   ├── BackgroundPrefs.kt       背景图与强度（图片存应用私有目录）
│   │   ├── model/                   数据模型（Course / Override / SemesterConfig…）
│   │   ├── db/                      Room 实体、DAO、数据库（v2，含迁移）
│   │   └── engine/                  ScheduleEngine：纯函数计算引擎
│   ├── ui/
│   │   ├── SetupWizardScreen.kt     首启向导（开学日期 + 作息）
│   │   ├── HomeScreen.kt            主页：周视图 + 各类操作
│   │   ├── WeekView.kt              周视图网格渲染
│   │   ├── AddCourseScreen.kt       手动添加课程
│   │   ├── ImportScreen.kt          从教务导入（WebView）
│   │   ├── SettingsScreen.kt        设置 / 关于
│   │   └── TeachingScreen.kt        使用教学
│   └── widget/
│       ├── TodayWidgetProvider.kt   桌面小组件（RemoteViews 同步渲染）
│       └── WidgetScheduler.kt       WorkManager 定时刷新
├── assets/adapters/
│   └── adapter.js                   课表适配脚本（换学校只改这一个文件）
└── res/
    ├── layout/widget_today.xml      小组件布局
    ├── drawable/widget_*.xml        小组件背景与箭头反馈
    └── values/widget_styles.xml     小组件文字样式
```

---

## 换学校 / 自己适配

课表解析与 App 本体**完全解耦**，只需要替换一个文件：

```
app/src/main/assets/adapters/adapter.js
```

它要满足的接口很窄 —— 解析出数据后调三个桥接方法即可：

```js
kcbBridge.saveCourses(JSON.stringify({ semester, maxWeek, courses }));
kcbBridge.notifyTaskCompletion();   // 完成
kcbBridge.reportError("消息");       // 出错（只弹提示，不影响 App 本体）
```

`courses` 里每条课程的字段：

| 字段 | 含义 |
| --- | --- |
| `name` | 课程名 |
| `teacher` / `position` / `campus` | 教师 / 地点 / 校区 |
| `day` | 星期几，1 = 周一 … 7 = 周日 |
| `startSection` / `endSection` | 起止节次 |
| `weeks` | 周次整数数组，如 `[1,2,3,8]` |

自带的 `adapter.js` 是按「**正方教务 jwglxt 列表版课表**」写的，判断依据是：

- 课表页形如 `.../kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N2151`
- 表格 `id` 为 `kblist_table`，每周一个 `tbody`，`id` 依次为 `xq_1` ~ `xq_7`
- 每门课是 `.timetable_con`，课名在其中的 `.title`

如果目标学校用的是「**格子版**」课表（表格 `id` = `kbgrid_table_0`），这份脚本不能直接用，需要另写一份。

**WebVPN 地址不用改代码** —— 在 App 的导入页里直接填，会自动记住。

---

## 核心设计

### 调休模型（A + B 混合）

- **A 日期重映射**：`DateRemap(targetDate, sourceWeekday?)` —— 表达「某天按星期几的课表执行」。目标永远指向**原始星期几**而不是另一个日期，所以天然不会链式递归；`sourceWeekday = null` 表示当天放假。
- **B 实例覆盖**：`InstanceOverride(date, ADD/REMOVE, snapshot, originDate)` —— 单节课的增删改，带来源标记以便显示角标与撤销。

优先级唯一顺序：**实例覆盖 > 日期重映射 > 原始课程**。

所有覆盖**只追加、永不修改原始数据**，所以「撤销」就是删掉一条记录，渲染时重新合成。

### 周次用集合，不用规则

课程只存 `Set<Int>`。起止周、单双周这些规则**只活在解析层的正则里**，用完就丢。

---

## 构建

需要 JDK 17、Android SDK（compileSdk 35）。

```bash
# local.properties 里指定 sdk.dir
./gradlew assembleDebug
```

> ⚠️ 注意：本项目路径不能含非 ASCII 字符（AGP 会直接拒绝）。

发布版签名信息从项目根目录的 `keystore.properties` 读取（该文件与 `*.jks` 均已加入 `.gitignore`，不会入库）。

---

## 许可

仅供学习交流使用。
