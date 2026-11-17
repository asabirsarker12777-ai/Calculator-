package com.example.domain.model

enum class AngleMode {
    DEG,
    RAD
}

enum class HapticStrength(val label: String) {
    OFF("Off"),
    LIGHT("Light"),
    MEDIUM("Medium"),
    STRONG("Strong")
}

enum class AppThemeMode(val label: String) {
    DARK("Dark (ColorOS)"),
    LIGHT("Light Glass"),
    SYSTEM("System Default")
}

enum class AccentColorOption(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val glowHex: Long
) {
    AMBER_ORANGE("Liquid Amber", 0xFFFF7A18, 0xFFE64800, 0xFFFF9A44),
    COBALT_BLUE("Cobalt Glass", 0xFF3B82F6, 0xFF1D4ED8, 0xFF60A5FA),
    EMERALD_MINT("Emerald Mint", 0xFF10B981, 0xFF047857, 0xFF34D399),
    CRIMSON_CORAL("Crimson Coral", 0xFFF43F5E, 0xFFBE123C, 0xFFFB7185),
    VIOLET_GLASS("Prism Violet", 0xFF8B5CF6, 0xFF6D28D9, 0xFFA78BFA)
}

enum class NumberFormatStyle(val label: String, val example: String) {
    STANDARD("Standard", "1,234.56"),
    PLAIN("No Grouping", "1234.56"),
    EUROPEAN("Continental", "1.234,56")
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accentColor: AccentColorOption = AccentColorOption.AMBER_ORANGE,
    val hapticStrength: HapticStrength = HapticStrength.MEDIUM,
    val decimalPrecision: Int = 8,
    val numberFormatStyle: NumberFormatStyle = NumberFormatStyle.STANDARD,
    val soundEffectsEnabled: Boolean = false,
    val angleMode: AngleMode = AngleMode.DEG,
    val isScientificExpanded: Boolean = false
)

data class CalculationHistoryItem(
    val id: Long = 0,
    val expression: String,
    val result: String,
    val rawResult: String,
    val timestamp: Long,
    val isScientific: Boolean = false
)

data class CurrencyRate(
    val code: String,
    val name: String,
    val symbol: String,
    val regionBadge: String,
    val ratePerUsd: Double,
    val isFavorite: Boolean = false,
    val isManuallyEdited: Boolean = false,
    val lastUpdatedMillis: Long
)

data class RecentConversion(
    val id: Long = 0,
    val fromCode: String,
    val toCode: String,
    val amount: Double,
    val convertedAmount: Double,
    val timestamp: Long
)
