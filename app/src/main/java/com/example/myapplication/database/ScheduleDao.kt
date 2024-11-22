package com.example.myapplication.database

import androidx.room.*

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: Schedule)

    @Query("SELECT * FROM schedules ORDER BY creationTime DESC")
    suspend fun getAllSchedules(): List<Schedule>


    @Delete
    suspend fun delete(schedule: Schedule)
}
