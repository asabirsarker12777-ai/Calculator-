package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.RecentConversionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Query("SELECT * FROM currency_rates ORDER BY isFavorite DESC, code ASC")
    fun getAllRates(): Flow<List<CurrencyRateEntity>>

    @Query("SELECT * FROM currency_rates")
    suspend fun getAllRatesOnce(): List<CurrencyRateEntity>

    @Query("SELECT COUNT(*) FROM currency_rates")
    suspend fun getRateCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRates(rates: List<CurrencyRateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: CurrencyRateEntity)

    @Query("UPDATE currency_rates SET isFavorite = :isFavorite WHERE code = :code")
    suspend fun setFavorite(code: String, isFavorite: Boolean)

    @Query("SELECT * FROM recent_conversions ORDER BY timestamp DESC LIMIT 12")
    fun getRecentConversions(): Flow<List<RecentConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentConversion(conversion: RecentConversionEntity)

    @Query("DELETE FROM recent_conversions")
    suspend fun clearRecentConversions()
}
