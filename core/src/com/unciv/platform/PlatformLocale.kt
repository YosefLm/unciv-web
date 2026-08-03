package com.unciv.platform

import java.util.Locale
import yairm210.purity.annotations.Readonly

/** JVM locale construction kept behind a web overlay. */
object PlatformLocale {
    @JvmStatic
    @Readonly
    fun fromLanguageTag(languageTag: String): Locale = Locale.forLanguageTag(languageTag)
}
