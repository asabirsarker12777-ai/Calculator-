package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val code: String,
    val name: String,
    val symbol: String,
    val regionBadge: String,
    val ratePerUsd: Double,
    val isFavorite: Boolean = false,
    val isManuallyEdited: Boolean = false,
    val lastUpdatedMillis: Long
)
