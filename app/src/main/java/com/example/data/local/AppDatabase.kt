package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CurrencyDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.local.entity.RecentConversionEntity

@Database(
    entities = [
        HistoryEntity::class,
        CurrencyRateEntity::class,
        RecentConversionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun currencyDao(): CurrencyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "abir_calculator.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
