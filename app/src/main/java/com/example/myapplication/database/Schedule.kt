package com.example.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String = "",
    val description: String = "",
    val inputTime: String = "00:00",
    val creationTime: String
)
