package com.firefly_liu.kcb.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    suspend fun getAll(): List<CourseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CourseEntity>)

    @Query("DELETE FROM courses")
    suspend fun clear()

    /** 重新导入前必须先清空，否则课表会叠加 */
    @Transaction
    suspend fun replaceAll(items: List<CourseEntity>) {
        clear()
        insertAll(items)
    }
}

@Dao
interface OverrideDao {
    @Query("SELECT * FROM date_remaps")
    suspend fun getDateRemaps(): List<DateRemapEntity>

    @Query("SELECT * FROM instance_overrides")
    suspend fun getInstanceOverrides(): List<InstanceOverrideEntity>

    @Insert
    suspend fun insertDateRemap(e: DateRemapEntity): Long

    @Insert
    suspend fun insertInstanceOverride(e: InstanceOverrideEntity): Long

    @Delete
    suspend fun deleteDateRemap(e: DateRemapEntity)

    @Delete
    suspend fun deleteInstanceOverride(e: InstanceOverrideEntity)

    @Query("DELETE FROM date_remaps")
    suspend fun clearDateRemaps()

    @Query("DELETE FROM instance_overrides")
    suspend fun clearInstanceOverrides()
}

@Dao
interface ConfigDao {
    @Query("SELECT * FROM semester_config WHERE id = 1")
    suspend fun get(): SemesterConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(e: SemesterConfigEntity)

    @Query("DELETE FROM semester_config")
    suspend fun clear()
}
