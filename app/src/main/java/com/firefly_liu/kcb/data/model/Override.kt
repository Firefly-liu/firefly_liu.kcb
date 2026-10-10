package com.firefly_liu.kcb.data.model

import java.time.LocalDate

/**
 * 覆盖层：所有覆盖「只追加、永不修改原始数据」。
 * 撤销 = 删除记录；渲染 = 原始数据 + 覆盖合成。
 */
sealed interface Override {
    val id: Long
    val createdAt: Long
}

/**
 * A：日期重映射。
 * [sourceWeekday] 指向「原始星期几」而不是另一个日期 —— 因此天然不会链式递归。
 * 为 null 表示当天停课/放假（放假与补课共用同一模型）。
 */
data class DateRemap(
    override val id: Long = 0L,
    val targetDate: LocalDate,
    val sourceWeekday: Int?,
    val note: String = "",
    override val createdAt: Long = 0L,
) : Override

enum class OverrideAction { ADD, REMOVE }

/** B：实例覆盖，针对单节课的增删，带来源标记 */
data class InstanceOverride(
    override val id: Long = 0L,
    val date: LocalDate,
    val action: OverrideAction,
    val targetCourseId: Long? = null,
    val snapshot: CourseSnapshot? = null,
    val originDate: LocalDate? = null,
    val originLabel: String = "",
    override val createdAt: Long = 0L,
) : Override

/** 复制/移动时冻结的课程内容 */
data class CourseSnapshot(
    val name: String,
    val teacher: String = "",
    val position: String = "",
    val campus: String = "",
    val startSection: Int,
    val endSection: Int,
    val customStartMin: Int? = null,
    val customEndMin: Int? = null,
) {
    companion object {
        fun of(c: CourseInstance) = CourseSnapshot(
            name = c.name,
            teacher = c.teacher,
            position = c.position,
            campus = c.campus,
            startSection = c.startSection,
            endSection = c.endSection,
            customStartMin = c.customStartMin,
            customEndMin = c.customEndMin,
        )
    }
}


