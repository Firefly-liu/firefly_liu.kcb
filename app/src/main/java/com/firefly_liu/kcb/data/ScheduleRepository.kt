package com.firefly_liu.kcb.data

import com.firefly_liu.kcb.data.db.AppDatabase
import com.firefly_liu.kcb.data.db.CourseEntity
import com.firefly_liu.kcb.data.db.DateRemapEntity
import com.firefly_liu.kcb.data.db.InstanceOverrideEntity
import com.firefly_liu.kcb.data.db.SemesterConfigEntity
import com.firefly_liu.kcb.data.engine.ScheduleEngine
import com.firefly_liu.kcb.data.model.Course
import com.firefly_liu.kcb.data.model.CourseInstance
import com.firefly_liu.kcb.data.model.DateRemap
import com.firefly_liu.kcb.data.model.InstanceOverride
import com.firefly_liu.kcb.data.model.Override
import com.firefly_liu.kcb.data.model.SemesterConfig
import java.time.LocalDate

/** 把持久化和计算引擎串起来 */
class ScheduleRepository(private val db: AppDatabase) {

    suspend fun loadCourses(): List<Course> =
        db.courseDao().getAll().map { it.toDomain() }

    /**
     * 重新导入：先清空再写入。
     * 同时清掉旧的调休记录 —— 它们引用的是上一份课表，留着会和新课表串味。
     */
    suspend fun replaceCourses(list: List<Course>) {
        db.courseDao().replaceAll(list.map { CourseEntity.from(it) })
        db.overrideDao().clearDateRemaps()
        db.overrideDao().clearInstanceOverrides()
    }

    /** 手动添加单门课程 */
    suspend fun addCourse(c: Course) {
        db.courseDao().insertAll(listOf(CourseEntity.from(c)))
    }

    suspend fun loadOverrides(): List<Override> {
        val remaps = db.overrideDao().getDateRemaps().map { it.toDomain() }
        val instances = db.overrideDao().getInstanceOverrides().map { it.toDomain() }
        return remaps + instances
    }

    suspend fun addDateRemap(r: DateRemap) {
        db.overrideDao().insertDateRemap(DateRemapEntity.from(r))
    }

    suspend fun addInstanceOverride(o: InstanceOverride) {
        db.overrideDao().insertInstanceOverride(InstanceOverrideEntity.from(o))
    }

    /** 撤销一条调整记录 */
    suspend fun removeOverride(o: Override) {
        when (o) {
            is DateRemap -> db.overrideDao().deleteDateRemap(DateRemapEntity.from(o))
            is InstanceOverride -> db.overrideDao().deleteInstanceOverride(InstanceOverrideEntity.from(o))
        }
    }

    suspend fun loadConfig(): SemesterConfig? = db.configDao().get()?.toDomain()

    suspend fun saveConfig(c: SemesterConfig) {
        db.configDao().save(SemesterConfigEntity.from(c))
    }

    suspend fun clearConfig() {
        db.configDao().clear()
    }

    suspend fun instancesOf(date: LocalDate): List<CourseInstance> {
        val config = loadConfig() ?: return emptyList()
        return ScheduleEngine.instancesOf(date, loadCourses(), loadOverrides(), config)
    }
}



