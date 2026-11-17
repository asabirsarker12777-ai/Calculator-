package com.example.data.local

import com.example.data.local.entity.CurrencyRateEntity

object PreloadedCurrencyData {
    // Reference timestamp: Nov 16, 2026 (epoch ms)
    const val DEFAULT_REFERENCE_TIMESTAMP_MS = 1794812800000L

    fun getDefaultRates(timestampMs: Long = DEFAULT_REFERENCE_TIMESTAMP_MS): List<CurrencyRateEntity> {
        return listOf(
            CurrencyRateEntity("USD", "US Dollar", "$", "US", 1.0, isFavorite = true, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("BDT", "Bangladeshi Taka", "৳", "BD", 119.50, isFavorite = true, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("EUR", "Euro", "€", "EU", 0.925, isFavorite = true, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("GBP", "British Pound", "£", "UK", 0.785, isFavorite = true, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("INR", "Indian Rupee", "₹", "IN", 84.40, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("AED", "UAE Dirham", "د.إ", "AE", 3.6725, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("SAR", "Saudi Riyal", "﷼", "SA", 3.75, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("CAD", "Canadian Dollar", "C$", "CA", 1.39, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("AUD", "Australian Dollar", "A$", "AU", 1.53, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("JPY", "Japanese Yen", "¥", "JP", 154.20, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("CNY", "Chinese Yuan", "¥", "CN", 7.22, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("SGD", "Singapore Dollar", "S$", "SG", 1.34, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("MYR", "Malaysian Ringgit", "RM", "MY", 4.44, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("PKR", "Pakistani Rupee", "₨", "PK", 277.80, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("CHF", "Swiss Franc", "CHF", "CH", 0.882, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("KRW", "South Korean Won", "₩", "KR", 1395.0, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("QAR", "Qatari Riyal", "QR", "QA", 3.64, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("KWD", "Kuwaiti Dinar", "KD", "KW", 0.307, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("THB", "Thai Baht", "฿", "TH", 34.60, isFavorite = false, lastUpdatedMillis = timestampMs),
            CurrencyRateEntity("TRY", "Turkish Lira", "₺", "TR", 34.45, isFavorite = false, lastUpdatedMillis = timestampMs)
        )
    }

    fun getDefaultRateForCode(code: String): Double? {
        return getDefaultRates().firstOrNull { it.code == code }?.ratePerUsd
    }
}
