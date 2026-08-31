package com.alpaca.app.data.content

import java.util.Locale

/**
 * A course language. Content files are bundled as `assets/content/<id>_unitN.json`.
 */
data class CourseLanguage(
    val id: String,
    val displayName: String,
    val nativeName: String,
    val flagEmoji: String,
    val speechTag: String
) {
    val ttsLocale: Locale get() = Locale.forLanguageTag(speechTag)

    companion object {
        val Spanish = CourseLanguage("es", "Spanish", "Español", "🇪🇸", "es-ES")
        val French = CourseLanguage("fr", "French", "Français", "🇫🇷", "fr-FR")
        val German = CourseLanguage("de", "German", "Deutsch", "🇩🇪", "de-DE")
        val Italian = CourseLanguage("it", "Italian", "Italiano", "🇮🇹", "it-IT")
        val Portuguese = CourseLanguage("pt", "Portuguese", "Português", "🇵🇹", "pt-PT")

        val available = listOf(Spanish, French, German, Italian, Portuguese)

        // Shown locked in the course picker, Duolingo-style.
        val comingSoon = listOf(
            CourseLanguage("ja", "Japanese", "日本語", "🇯🇵", "ja-JP")
        )

        fun byId(id: String): CourseLanguage =
            available.firstOrNull { it.id == id } ?: Spanish
    }
}
