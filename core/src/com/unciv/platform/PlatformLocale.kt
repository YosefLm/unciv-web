package com.unciv.platform

import java.util.Locale

/** JVM locale construction kept behind a web overlay. */
object PlatformLocale {
    @JvmStatic
    fun fromLanguageTag(languageTag: String): Locale = Locale.forLanguageTag(languageTag)
}
