package com.firefly_liu.kcb.engine

import com.firefly_liu.kcb.data.engine.ScheduleEngine
import com.firefly_liu.kcb.data.model.Course
import com.firefly_liu.kcb.data.model.CourseSnapshot
import com.firefly_liu.kcb.data.model.DateRemap
import com.firefly_liu.kcb.data.model.InstanceOrigin
import com.firefly_liu.kcb.data.model.InstanceOverride
import com.firefly_liu.kcb.data.model.OverrideAction
import com.firefly_liu.kcb.data.model.SemesterConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleEngineTest {

    /** 第 1 周周一 */
    private val start: LocalDate = LocalDate.of(2026, 9, 7)
    private val config = SemesterConfig(startDate = start, totalWeeks = 16)

    private fun course(
        id: Long, name: String, day: Int, ss: Int, es: Int, weeks: IntRange,
    ) = Course(
        id = id, name = name, day = day,
        startSection = ss, endSection = es,
        weeks = weeks.toSet(),
    )

    private val monA = course(1, "工业机器人", 1, 1, 2, 1..16)
    private val satA = course(2, "周六课", 6, 1, 2, 1..16)
    private val tueB = course(3, "前半学期课", 2, 5, 6, 1..8)

    @Test
    fun weekOf_isCorrect() {
        assertEquals(1, ScheduleEngine.weekOf(start, start))
        assertEquals(1, ScheduleEngine.weekOf(start.plusDays(6), start))
        assertEquals(2, ScheduleEngine.weekOf(start.plusDays(7), start))
        assertEquals(3, ScheduleEngine.weekOf(start.plusDays(14), start))
    }

    @Test
    fun originalCourses_areFilteredByWeekdayAndWeek() {
        val tue = start.plusDays(1)
        val list = ScheduleEngine.instancesOf(tue, listOf(monA, satA, tueB), emptyList(), config)
        assertEquals(1, list.size)
        assertEquals("前半学期课", list[0].name)
        assertEquals(InstanceOrigin.NORMAL, list[0].origin)
    }

    @Test
    fun coursesOutsideWeekRange_doNotAppear() {
        val lateTue = start.plusWeeks(9).plusDays(1)
        val list = ScheduleEngine.instancesOf(lateTue, listOf(monA, satA, tueB), emptyList(), config)
        assertTrue(list.isEmpty())
    }

    @Test
    fun dateRemap_saturdayFollowsMonday() {
        val sat = start.plusDays(5)
        val remap = DateRemap(targetDate = sat, sourceWeekday = 1, note = "tiaoxiu")
        val list = ScheduleEngine.instancesOf(sat, listOf(monA, satA), listOf(remap), config)
        assertEquals(1, list.size)
        assertEquals("工业机器人", list[0].name)
        assertEquals(InstanceOrigin.REMAPPED, list[0].origin)
    }

    @Test
    fun dateRemapWithNullWeekday_clearsTheDay() {
        val mon = start
        val remap = DateRemap(targetDate = mon, sourceWeekday = null, note = "holiday")
        val list = ScheduleEngine.instancesOf(mon, listOf(monA, satA), listOf(remap), config)
        assertTrue(list.isEmpty())
    }

    @Test
    fun instanceOverride_add_appendsToDay() {
        val sat = start.plusDays(5)
        val add = InstanceOverride(
            date = sat,
            action = OverrideAction.ADD,
            snapshot = CourseSnapshot(name = "临时补课", startSection = 3, endSection = 4),
            originDate = start.plusDays(1),
            originLabel = "从周二移动",
        )
        val list = ScheduleEngine.instancesOf(sat, listOf(monA, satA), listOf(add), config)
        assertEquals(2, list.size)
        assertEquals("周六课", list[0].name)
        assertEquals("临时补课", list[1].name)
        assertEquals(InstanceOrigin.OVERRIDE, list[1].origin)
    }

    @Test
    fun instanceOverride_remove_deletesCourse() {
        val mon = start
        val del = InstanceOverride(
            date = mon,
            action = OverrideAction.REMOVE,
            targetCourseId = 1,
        )
        val list = ScheduleEngine.instancesOf(mon, listOf(monA, satA), listOf(del), config)
        assertTrue(list.isEmpty())
    }

    @Test
    fun priority_instanceOverrideBeatsDateRemap() {
        val sat = start.plusDays(5)
        val remap = DateRemap(targetDate = sat, sourceWeekday = 1)
        val del = InstanceOverride(
            date = sat,
            action = OverrideAction.REMOVE,
            targetCourseId = 1,
        )
        val list = ScheduleEngine.instancesOf(sat, listOf(monA, satA), listOf(remap, del), config)
        assertTrue(list.isEmpty())
    }

    @Test
    fun dateRemap_doesNotRecurse() {
        val mon = start
        val sat = start.plusDays(5)
        val r1 = DateRemap(targetDate = sat, sourceWeekday = 1)
        val r2 = DateRemap(targetDate = mon, sourceWeekday = 6)
        val satList = ScheduleEngine.instancesOf(sat, listOf(monA, satA), listOf(r1, r2), config)
        assertEquals("工业机器人", satList[0].name)
        val monList = ScheduleEngine.instancesOf(mon, listOf(monA, satA), listOf(r1, r2), config)
        assertEquals("周六课", monList[0].name)
    }
}

