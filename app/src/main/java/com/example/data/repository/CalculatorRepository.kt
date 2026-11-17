package com.example.data.repository

import com.example.data.local.dao.HistoryDao
import com.example.data.local.entity.HistoryEntity
import com.example.domain.model.CalculationHistoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CalculatorRepository(private val historyDao: HistoryDao) {

    fun observeHistory(query: String = ""): Flow<List<CalculationHistoryItem>> {
        val source = if (query.isBlank()) {
            historyDao.getAllHistory()
        } else {
            historyDao.searchHistory(query.trim())
        }
        return source.map { list ->
            list.map { entity ->
                CalculationHistoryItem(
                    id = entity.id,
                    expression = entity.expression,
                    result = entity.result,
                    rawResult = entity.rawResult,
                    timestamp = entity.timestamp,
                    isScientific = entity.isScientific
                )
            }
        }
    }

    suspend fun saveCalculation(
        expression: String,
        formattedResult: String,
        rawResult: String,
        isScientific: Boolean
    ) {
        historyDao.insertHistory(
            HistoryEntity(
                expression = expression,
                result = formattedResult,
                rawResult = rawResult,
                timestamp = System.currentTimeMillis(),
                isScientific = isScientific
            )
        )
    }

    suspend fun deleteHistoryItem(id: Long) {
        historyDao.deleteById(id)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }
}
