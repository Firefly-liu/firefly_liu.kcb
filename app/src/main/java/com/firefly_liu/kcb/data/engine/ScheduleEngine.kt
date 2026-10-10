package com.firefly_liu.kcb.data.engine

import com.firefly_liu.kcb.data.model.Course
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.DateRemap
import com.firefly_liu.kcb.data.model.InstanceOrigin
import com.firefly_liu.kcb.data.model.InstanceOverride
import com.firefly_liu.kcb.data.model.Override
import com.firefly_liu.kcb.data.model.OverrideAction
import com.firefly_liu.kcb.data.model.SemesterConfig
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 计算引擎：纯函数，无副作用、不依赖 Android。
 *
 * 优先级（唯一顺序）：InstanceOverride > DateRemap > 原始 Course
 */
object ScheduleEngine {

    /** 第几周（从 1 开始）。[startDate] 为第 1 周周一 */
    fun weekOf(date: LocalDate, startDate: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(startDate, date)
        return Math.floorDiv(days, 7L).toInt() + 1
    }

    /** 某一天的课程实例 */
    fun instancesOf(
        date: LocalDate,
        courses: List<Course>,
        overrides: List<Override>,
        config: SemesterConfig,
    ): List<CourseInstance> {
        val weekNo = weekOf(date, config.startDate)
        if (weekNo < 1) return emptyList()

        // 第一层：DateRemap（取最后创建的一条，新的覆盖旧的）
        val remap = overrides.filterIsInstance<DateRemap>()
            .lastOrNull { it.targetDate == date }

        val base: List<CourseInstance> = when {
            remap == null ->
                courses.filter { it.day == date.dayOfWeek.value && it.isActiveIn(weekNo) }
                    .map { it.toInstance(date, InstanceOrigin.NORMAL) }

            remap.sourceWeekday == null -> emptyList()

            else ->
                courses.filter { it.day == remap.sourceWeekday && it.isActiveIn(weekNo) }
                    .map { it.toInstance(date, InstanceOrigin.REMAPPED) }
        }

        // 第二层：InstanceOverride
        val result = base.toMutableList()
        overrides.filterIsInstance<InstanceOverride>()
            .filter { it.date == date }
            .sortedBy { it.createdAt }
            .forEach { ov ->
                when (ov.action) {
                    OverrideAction.REMOVE -> result.removeAll { inst -> matches(inst, ov) }
                    OverrideAction.ADD -> ov.snapshot?.let { snap ->
                        result.add(
                            CourseInstance(
                                name = snap.name,
                                teacher = snap.teacher,
                                position = snap.position,
                                campus = snap.campus,
                                date = date,
                                startSection = snap.startSection,
                                endSection = snap.endSection,
                                sourceCourseId = ov.targetCourseId,
                                origin = InstanceOrigin.OVERRIDE,
                                note = ov.originLabel,
                                customStartMin = snap.customStartMin,
                                customEndMin = snap.customEndMin,
                            )
                        )
                    }
                }
            }

        return result.sortedWith(compareBy({ it.startSection }, { it.name }))
    }

    /** 某一周（周一到周日）的课程实例，按天分组 */
    fun weekInstances(
        weekNo: Int,
        courses: List<Course>,
        overrides: List<Override>,
        config: SemesterConfig,
    ): Map<LocalDate, List<CourseInstance>> {
        val monday = config.startDate.plusWeeks((weekNo - 1).toLong())
        return (0..6).associate { offset ->
            val d = monday.plusDays(offset.toLong())
            d to instancesOf(d, courses, overrides, config)
        }
    }

    private fun matches(inst: CourseInstance, ov: InstanceOverride): Boolean {
        val target = ov.targetCourseId
        if (target != null && inst.sourceCourseId == target) return true
        val snap = ov.snapshot
        return snap != null &&
            inst.name == snap.name &&
            inst.startSection == snap.startSection &&
            inst.endSection == snap.endSection
    }

    private fun Course.toInstance(date: LocalDate, origin: InstanceOrigin) = CourseInstance(
        name = name,
        teacher = teacher,
        position = position,
        campus = campus,
        date = date,
        startSection = startSection,
        endSection = endSection,
        sourceCourseId = id,
        origin = origin,
        customStartMin = customStartMin,
        customEndMin = customEndMin,
    )
}


