package com.unciv.platform

import java.util.Locale

/** TeaVM-compatible locale subset used by Unciv. */
object PlatformLocale {
    @JvmStatic
    fun fromLanguageTag(languageTag: String): Locale {
        val parts = languageTag.split('-')
        val language = parts.firstOrNull()?.takeIf { it.isNotEmpty() } ?: return Locale.getDefault()
        val country = parts.drop(1).firstOrNull { it.length == 2 && it.all(Char::isLetter) }.orEmpty()
        return if (country.isEmpty()) Locale(language) else Locale(language, country)
    }
}
