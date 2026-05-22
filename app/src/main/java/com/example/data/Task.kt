package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val category: String = "Personal",
    val priority: String = "Medium", // "Low", "Medium", "High"
    val dueDate: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
