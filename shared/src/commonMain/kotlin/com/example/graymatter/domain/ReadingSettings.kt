package com.example.graymatter.domain

/**
 * Per-document display preferences.
 */
data class ReadingSettings(
    val resourceId: String,
    val fontSize: Int = 16,
    val typeface: String = "system",
    val lineSpacing: Double = 1.5,
    val margins: Int = 16,
    val theme: String = "daylight",    // daylight, paper_classic, book_linen, snow_contrast, night, night_contrast, amoled_black, twilight, console, sepia, sepia_contrast, vintage_parchment
    val scrollMode: String = "paged",  // "paged", "continuous", "auto"
    val brightness: Double = -1.0,     // -1 = system default
    val keepScreenOn: Boolean = false,
    val textReflow: Boolean = true,
    val autoCrop: Boolean = true       // Defaulted to true
)
