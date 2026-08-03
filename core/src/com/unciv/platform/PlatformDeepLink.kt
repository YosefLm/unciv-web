package com.unciv.platform

import com.unciv.utils.Log
import io.ktor.http.URLParserException
import io.ktor.http.Url
import yairm210.purity.annotations.Pure

data class DeepLinkParts(val segments: List<String>, val name: String)

/** JVM deep-link parser kept behind a browser-safe overlay. */
@Pure
fun parseUncivDeepLink(url: String): DeepLinkParts? = try {
    val parsed = Url(url)
    DeepLinkParts(parsed.segments, parsed.parameters["name"].orEmpty())
} catch (exception: URLParserException) {
    Log.error("invalid format for url $url", exception)
    null
}
