package com.firefly_liu.kcb.data.model

import java.time.LocalDate
import java.time.LocalTime

/** 节次时间（第几节 -> 起止时间），由用户手动配置 */
data class ClassSlot(
    val number: Int,
    val start: LocalTime,
    val end: LocalTime,
)

/** 学期配置：用户手动填写，抓取拿不到 */
data class SemesterConfig(
    val startDate: LocalDate,
    val totalWeeks: Int,
    val slots: List<ClassSlot> = emptyList(),
) {
    companion object {
        /** 预置作息模板（10 节），用户可改 */
        fun defaultSlots(): List<ClassSlot> = listOf(
            ClassSlot(1, LocalTime.of(8, 0), LocalTime.of(8, 45)),
            ClassSlot(2, LocalTime.of(8, 55), LocalTime.of(9, 40)),
            ClassSlot(3, LocalTime.of(10, 0), LocalTime.of(10, 45)),
            ClassSlot(4, LocalTime.of(10, 55), LocalTime.of(11, 40)),
            ClassSlot(5, LocalTime.of(13, 30), LocalTime.of(14, 15)),
            ClassSlot(6, LocalTime.of(14, 25), LocalTime.of(15, 10)),
            ClassSlot(7, LocalTime.of(15, 30), LocalTime.of(16, 15)),
            ClassSlot(8, LocalTime.of(16, 25), LocalTime.of(17, 10)),
            ClassSlot(9, LocalTime.of(18, 30), LocalTime.of(19, 15)),
            ClassSlot(10, LocalTime.of(19, 25), LocalTime.of(20, 10)),
        )
    }
}

/**
 * 课程定义：由适配脚本抓取产生，之后只读。
 * weeks 是「具体周次集合」而非起止周/单双周规则 —— 规则只活在解析层。
 */
data class Course(
    val id: Long = 0L,
    val name: String,
    val teacher: String = "",
    val position: String = "",
    val campus: String = "",
    val day: Int,
    val startSection: Int,
    val endSection: Int,
    val weeks: Set<Int> = emptySet(),
    val credit: Double = 0.0,
    val nature: String = "",
    val assessment: String = "",
    val source: String = "",
    /** 自定义时间课程：一天内的分钟数（null 表示按节次） */
    val customStartMin: Int? = null,
    val customEndMin: Int? = null,
) {
    fun isActiveIn(weekNo: Int): Boolean = weekNo in weeks
}

/** 实例来源，用于 UI 打角标 */
enum class InstanceOrigin { NORMAL, REMAPPED, OVERRIDE }

/** 计算产物：不落库，由引擎按需生成 */
data class CourseInstance(
    val name: String,
    val teacher: String = "",
    val position: String = "",
    val campus: String = "",
    val date: LocalDate,
    val startSection: Int,
    val endSection: Int,
    val sourceCourseId: Long? = null,
    val origin: InstanceOrigin = InstanceOrigin.NORMAL,
    val note: String = "",
    val customStartMin: Int? = null,
    val customEndMin: Int? = null,
) {
    /** 自定义时间的显示文本，如 "19:00-21:00" */
    val customTimeLabel: String?
        get() = if (customStartMin != null && customEndMin != null) {
            "%02d:%02d-%02d:%02d".format(
                customStartMin / 60, customStartMin % 60,
                customEndMin / 60, customEndMin % 60,
            )
        } else null
    val sections: IntRange get() = startSection..endSection
}



