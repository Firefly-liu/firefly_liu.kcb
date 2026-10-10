package com.firefly_liu.kcb.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.firefly_liu.kcb.data.model.ClassSlot
import com.firefly_liu.kcb.data.model.Course
import com.firefly_liu.kcb.data.model.CourseSnapshot
import com.firefly_liu.kcb.data.model.DateRemap
import com.firefly_liu.kcb.data.model.InstanceOverride
import com.firefly_liu.kcb.data.model.OverrideAction
import com.firefly_liu.kcb.data.model.SemesterConfig
import java.time.LocalDate
import java.time.LocalTime

private fun parseWeeks(csv: String): Set<Int> =
    csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val teacher: String,
    val position: String,
    val campus: String,
    val day: Int,
    val startSection: Int,
    val endSection: Int,
    val weeksCsv: String,
    val credit: Double,
    val nature: String,
    val assessment: String,
    val source: String,
    val customStartMin: Int? = null,
    val customEndMin: Int? = null,
) {
    fun toDomain() = Course(
        id = id, name = name, teacher = teacher, position = position, campus = campus,
        day = day, startSection = startSection, endSection = endSection,
        weeks = parseWeeks(weeksCsv), credit = credit, nature = nature,
        assessment = assessment, source = source,
        customStartMin = customStartMin, customEndMin = customEndMin,
    )

    companion object {
        fun from(c: Course) = CourseEntity(
            id = c.id, name = c.name, teacher = c.teacher, position = c.position,
            campus = c.campus, day = c.day, startSection = c.startSection,
            endSection = c.endSection, weeksCsv = c.weeks.sorted().joinToString(","),
            credit = c.credit, nature = c.nature, assessment = c.assessment, source = c.source,
            customStartMin = c.customStartMin, customEndMin = c.customEndMin,
        )
    }
}

@Entity(tableName = "date_remaps")
data class DateRemapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val targetEpochDay: Long,
    val sourceWeekday: Int?,
    val note: String,
    val createdAt: Long,
) {
    fun toDomain() = DateRemap(
        id = id, targetDate = LocalDate.ofEpochDay(targetEpochDay),
        sourceWeekday = sourceWeekday, note = note, createdAt = createdAt,
    )

    companion object {
        fun from(r: DateRemap) = DateRemapEntity(
            id = r.id, targetEpochDay = r.targetDate.toEpochDay(),
            sourceWeekday = r.sourceWeekday, note = r.note, createdAt = r.createdAt,
        )
    }
}

@Entity(tableName = "instance_overrides")
data class InstanceOverrideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val epochDay: Long,
    val action: String,
    val targetCourseId: Long?,
    val snapName: String?,
    val snapTeacher: String?,
    val snapPosition: String?,
    val snapCampus: String?,
    val snapStartSection: Int?,
    val snapEndSection: Int?,
    val originEpochDay: Long?,
    val originLabel: String,
    val createdAt: Long,
) {
    fun toDomain() = InstanceOverride(
        id = id, date = LocalDate.ofEpochDay(epochDay),
        action = OverrideAction.valueOf(action), targetCourseId = targetCourseId,
        snapshot = if (snapName != null && snapStartSection != null && snapEndSection != null)
            CourseSnapshot(
                name = snapName, teacher = snapTeacher ?: "", position = snapPosition ?: "",
                campus = snapCampus ?: "", startSection = snapStartSection,
                endSection = snapEndSection,
            ) else null,
        originDate = originEpochDay?.let { LocalDate.ofEpochDay(it) },
        originLabel = originLabel, createdAt = createdAt,
    )

    companion object {
        fun from(o: InstanceOverride) = InstanceOverrideEntity(
            id = o.id, epochDay = o.date.toEpochDay(), action = o.action.name,
            targetCourseId = o.targetCourseId,
            snapName = o.snapshot?.name, snapTeacher = o.snapshot?.teacher,
            snapPosition = o.snapshot?.position, snapCampus = o.snapshot?.campus,
            snapStartSection = o.snapshot?.startSection, snapEndSection = o.snapshot?.endSection,
            originEpochDay = o.originDate?.toEpochDay(),
            originLabel = o.originLabel, createdAt = o.createdAt,
        )
    }
}

@Entity(tableName = "semester_config")
data class SemesterConfigEntity(
    @PrimaryKey val id: Int = 1,
    val startEpochDay: Long,
    val totalWeeks: Int,
    val slotsCsv: String,
) {
    fun toDomain() = SemesterConfig(
        startDate = LocalDate.ofEpochDay(startEpochDay),
        totalWeeks = totalWeeks,
        slots = parseSlots(slotsCsv),
    )

    companion object {
        fun from(c: SemesterConfig) = SemesterConfigEntity(
            startEpochDay = c.startDate.toEpochDay(),
            totalWeeks = c.totalWeeks,
            slotsCsv = c.slots.joinToString(";") { "${it.number},${it.start.toSecondOfDay()},${it.end.toSecondOfDay()}" },
        )

        private fun parseSlots(csv: String): List<ClassSlot> =
            csv.split(";").mapNotNull { part ->
                val f = part.split(",")
                if (f.size != 3) return@mapNotNull null
                val n = f[0].trim().toIntOrNull() ?: return@mapNotNull null
                val s = f[1].trim().toIntOrNull() ?: return@mapNotNull null
                val e = f[2].trim().toIntOrNull() ?: return@mapNotNull null
                ClassSlot(n, LocalTime.ofSecondOfDay(s.toLong()), LocalTime.ofSecondOfDay(e.toLong()))
            }
    }
}


