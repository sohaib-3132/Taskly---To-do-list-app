package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dueDate: String = "",
    val dueTimestamp: Long? = null,
    val priority: String = "Medium", // High, Medium, Low
    val category: String = "Personal", // Work, Personal, Urgent, etc.
    val description: String = "",
    val isCompleted: Boolean = false,
    val createdTimestamp: Long = System.currentTimeMillis()
)
