package com.example.data.repository

import com.example.data.local.PreloadedCurrencyData
import com.example.data.local.dao.CurrencyDao
import com.example.data.local.entity.CurrencyRateEntity
import com.example.data.local.entity.RecentConversionEntity
import com.example.data.remote.OnlineRateFetchResult
import com.example.data.remote.OnlineRateFetcher
import com.example.domain.model.CurrencyRate
import com.example.domain.model.RecentConversion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CurrencyRepository(
    private val currencyDao: CurrencyDao,
    private val onlineRateFetcher: OnlineRateFetcher
) {

    val ratesFlow: Flow<List<CurrencyRate>> = currencyDao.getAllRates().map { entities ->
        entities.map { it.toDomain() }
    }

    val recentConversionsFlow: Flow<List<RecentConversion>> =
        currencyDao.getRecentConversions().map { entities ->
            entities.map {
                RecentConversion(
                    id = it.id,
                    fromCode = it.fromCode,
                    toCode = it.toCode,
                    amount = it.amount,
                    convertedAmount = it.convertedAmount,
                    timestamp = it.timestamp
                )
            }
        }

    suspend fun ensureSeeded() {
        if (currencyDao.getRateCount() == 0) {
            currencyDao.upsertRates(PreloadedCurrencyData.getDefaultRates())
        }
    }

    suspend fun toggleFavorite(code: String, currentlyFavorite: Boolean) {
        currencyDao.setFavorite(code, !currentlyFavorite)
    }

    suspend fun updateManualRate(code: String, newRatePerUsd: Double) {
        if (newRatePerUsd <= 0.0 || newRatePerUsd.isNaN() || newRatePerUsd.isInfinite()) return
        val existing = currencyDao.getAllRatesOnce().firstOrNull { it.code == code } ?: return
        currencyDao.upsertRate(
            existing.copy(
                ratePerUsd = newRatePerUsd,
                isManuallyEdited = true,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetCurrencyToDefault(code: String) {
        val defaultRate = PreloadedCurrencyData.getDefaultRateForCode(code) ?: return
        val existing = currencyDao.getAllRatesOnce().firstOrNull { it.code == code } ?: return
        currencyDao.upsertRate(
            existing.copy(
                ratePerUsd = defaultRate,
                isManuallyEdited = false,
                lastUpdatedMillis = PreloadedCurrencyData.DEFAULT_REFERENCE_TIMESTAMP_MS
            )
        )
    }

    suspend fun saveRecentConversion(
        fromCode: String,
        toCode: String,
        amount: Double,
        convertedAmount: Double
    ) {
        if (amount <= 0.0) return
        currencyDao.insertRecentConversion(
            RecentConversionEntity(
                fromCode = fromCode,
                toCode = toCode,
                amount = amount,
                convertedAmount = convertedAmount,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearRecentConversions() {
        currencyDao.clearRecentConversions()
    }

    suspend fun refreshRatesOnline(overwriteManualEdits: Boolean = false): String {
        return when (val result = onlineRateFetcher.fetchLatestUsdRates()) {
            is OnlineRateFetchResult.Success -> {
                val current = currencyDao.getAllRatesOnce()
                val updated = current.map { entity ->
                    val fetched = result.ratesPerUsd[entity.code]
                    if (fetched != null && (overwriteManualEdits || !entity.isManuallyEdited)) {
                        entity.copy(
                            ratePerUsd = fetched,
                            isManuallyEdited = false,
                            lastUpdatedMillis = result.fetchedAtMillis
                        )
                    } else {
                        entity
                    }
                }
                currencyDao.upsertRates(updated)
                "Reference rates updated from online snapshot."
            }
            is OnlineRateFetchResult.Failure -> result.reason
        }
    }

    private fun CurrencyRateEntity.toDomain(): CurrencyRate = CurrencyRate(
        code = code,
        name = name,
        symbol = symbol,
        regionBadge = regionBadge,
        ratePerUsd = ratePerUsd,
        isFavorite = isFavorite,
        isManuallyEdited = isManuallyEdited,
        lastUpdatedMillis = lastUpdatedMillis
    )
}
