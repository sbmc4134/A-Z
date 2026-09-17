package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "ALL" for total overall monthly budget, or category name
    val monthYear: String, // e.g. "2026-08"
    val limitAmount: Double,
    val alertThresholdPercent: Int = 80 // Alert when spent >= 80%
)
