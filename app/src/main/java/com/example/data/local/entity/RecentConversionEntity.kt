package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_conversions")
data class RecentConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCode: String,
    val toCode: String,
    val amount: Double,
    val convertedAmount: Double,
    val timestamp: Long
)
