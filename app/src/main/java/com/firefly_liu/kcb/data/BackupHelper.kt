package com.firefly_liu.kcb.data

import com.firefly_liu.kcb.data.model.ClassSlot
import com.firefly_liu.kcb.data.model.Course
import com.firefly_liu.kcb.data.model.CourseSnapshot
import com.firefly_liu.kcb.data.model.DateRemap
import com.firefly_liu.kcb.data.model.InstanceOverride
import com.firefly_liu.kcb.data.model.OverrideAction
import com.firefly_liu.kcb.data.model.SemesterConfig
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/**
 * 备份与恢复：把课表 / 调休记录 / 学期设置打包成一个 JSON 文件。
 * 用 org.json 手写，不引额外序列化依赖。
 */
object BackupHelper {

    private const val FORMAT_VERSION = 1

    // ---------------- 导出 ----------------

    suspend fun exportJson(repo: ScheduleRepository): String {
        val root = JSONObject()
        root.put("format", "kcb-backup")
        root.put("version", FORMAT_VERSION)
        root.put("exportedAt", LocalDate.now().toString())

        repo.loadConfig()?.let { root.put("config", configToJson(it)) }
        root.put("courses", JSONArray().apply {
            repo.loadCourses().forEach { put(courseToJson(it)) }
        })

        val overrides = repo.loadOverrides()
        root.put("dateRemaps", JSONArray().apply {
            overrides.filterIsInstance<DateRemap>().forEach {
                put(JSONObject()
                    .put("targetDate", it.targetDate.toString())
                    .put("sourceWeekday", it.sourceWeekday ?: JSONObject.NULL)
                    .put("note", it.note)
                    .put("createdAt", it.createdAt))
            }
        })
        root.put("instanceOverrides", JSONArray().apply {
            overrides.filterIsInstance<InstanceOverride>().forEach {
                put(JSONObject()
                    .put("date", it.date.toString())
                    .put("action", it.action.name)
                    .put("targetCourseId", it.targetCourseId ?: JSONObject.NULL)
                    .put("snapshot", it.snapshot?.let { s -> snapshotToJson(s) } ?: JSONObject.NULL)
                    .put("originDate", it.originDate?.toString() ?: JSONObject.NULL)
                    .put("originLabel", it.originLabel)
                    .put("createdAt", it.createdAt))
            }
        })
        return root.toString(2)
    }

    private fun configToJson(c: SemesterConfig) = JSONObject()
        .put("startDate", c.startDate.toString())
        .put("totalWeeks", c.totalWeeks)
        .put("slots", JSONArray().apply {
            c.slots.forEach {
                put(JSONObject().put("number", it.number).put("start", it.start.toString()).put("end", it.end.toString()))
            }
        })

    private fun courseToJson(c: Course) = JSONObject()
        .put("name", c.name).put("teacher", c.teacher).put("position", c.position).put("campus", c.campus)
        .put("day", c.day).put("startSection", c.startSection).put("endSection", c.endSection)
        .put("weeks", JSONArray(c.weeks.sorted()))
        .put("credit", c.credit).put("nature", c.nature).put("assessment", c.assessment).put("source", c.source)
        .put("customStartMin", c.customStartMin ?: JSONObject.NULL)
        .put("customEndMin", c.customEndMin ?: JSONObject.NULL)

    private fun snapshotToJson(s: CourseSnapshot) = JSONObject()
        .put("name", s.name).put("teacher", s.teacher).put("position", s.position).put("campus", s.campus)
        .put("startSection", s.startSection).put("endSection", s.endSection)
        .put("customStartMin", s.customStartMin ?: JSONObject.NULL)
        .put("customEndMin", s.customEndMin ?: JSONObject.NULL)

    // ---------------- 导入 ----------------

    /** 返回导入的课程条数；格式不对会抛异常 */
    suspend fun importJson(repo: ScheduleRepository, json: String): Int {
        val root = JSONObject(json)
        require(root.optString("format") == "kcb-backup") { "这不是课程表的备份文件" }

        val courses = mutableListOf<Course>()
        root.optJSONArray("courses")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val weeks = mutableSetOf<Int>()
                o.optJSONArray("weeks")?.let { wa -> for (j in 0 until wa.length()) weeks.add(wa.getInt(j)) }
                courses.add(
                    Course(
                        name = o.optString("name"),
                        teacher = o.optString("teacher"),
                        position = o.optString("position"),
                        campus = o.optString("campus"),
                        day = o.optInt("day"),
                        startSection = o.optInt("startSection"),
                        endSection = o.optInt("endSection"),
                        weeks = weeks,
                        credit = o.optDouble("credit", 0.0),
                        nature = o.optString("nature"),
                        assessment = o.optString("assessment"),
                        source = o.optString("source"),
                        customStartMin = if (o.isNull("customStartMin")) null else o.getInt("customStartMin"),
                        customEndMin = if (o.isNull("customEndMin")) null else o.getInt("customEndMin"),
                    )
                )
            }
        }

        val remaps = mutableListOf<DateRemap>()
        root.optJSONArray("dateRemaps")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                remaps.add(
                    DateRemap(
                        targetDate = LocalDate.parse(o.getString("targetDate")),
                        sourceWeekday = if (o.isNull("sourceWeekday")) null else o.getInt("sourceWeekday"),
                        note = o.optString("note"),
                        createdAt = o.optLong("createdAt"),
                    )
                )
            }
        }

        val instances = mutableListOf<InstanceOverride>()
        root.optJSONArray("instanceOverrides")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val snapObj = o.optJSONObject("snapshot")
                instances.add(
                    InstanceOverride(
                        date = LocalDate.parse(o.getString("date")),
                        action = OverrideAction.valueOf(o.getString("action")),
                        targetCourseId = if (o.isNull("targetCourseId")) null else o.getLong("targetCourseId"),
                        snapshot = snapObj?.let {
                            CourseSnapshot(
                                name = it.optString("name"),
                                teacher = it.optString("teacher"),
                                position = it.optString("position"),
                                campus = it.optString("campus"),
                                startSection = it.optInt("startSection"),
                                endSection = it.optInt("endSection"),
                                customStartMin = if (it.isNull("customStartMin")) null else it.getInt("customStartMin"),
                                customEndMin = if (it.isNull("customEndMin")) null else it.getInt("customEndMin"),
                            )
                        },
                        originDate = if (o.isNull("originDate")) null else LocalDate.parse(o.getString("originDate")),
                        originLabel = o.optString("originLabel"),
                        createdAt = o.optLong("createdAt"),
                    )
                )
            }
        }

        // 先写配置，再整体替换课程与覆盖层
        root.optJSONObject("config")?.let { cfg ->
            val slots = mutableListOf<ClassSlot>()
            cfg.optJSONArray("slots")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    slots.add(
                        ClassSlot(
                            number = o.getInt("number"),
                            start = LocalTime.parse(o.getString("start")),
                            end = LocalTime.parse(o.getString("end")),
                        )
                    )
                }
            }
            repo.saveConfig(
                SemesterConfig(
                    startDate = LocalDate.parse(cfg.getString("startDate")),
                    totalWeeks = cfg.optInt("totalWeeks", 16),
                    slots = if (slots.isEmpty()) SemesterConfig.defaultSlots() else slots,
                )
            )
        }

        repo.replaceCourses(courses)
        repo.replaceOverrides(remaps, instances)
        return courses.size
    }
}
