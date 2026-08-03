package com.unciv.platform

import com.unciv.app.web.WebDeepLinkInterop

/** Small web URL parser for the only deep-link shape consumed by IdChecker. */
fun parseUncivDeepLink(url: String): DeepLinkParts? {
    val rawPath = url.substringBefore('?').substringBefore('#')
    val uncivPathStart = rawPath.indexOf("/Unciv/", ignoreCase = true)
    val path = when {
        uncivPathStart >= 0 -> rawPath.substring(uncivPathStart + 1)
        url.contains("://") -> rawPath.substringAfter("://").substringAfter('/', "")
        else -> rawPath
    }
    val segments = path.split('/').filter { it.isNotEmpty() }
    val query = url.substringAfter('?', "").substringBefore('#')
    val name = query.split('&')
        .firstOrNull { it.substringBefore('=') == "name" }
        ?.substringAfter('=', "")
        ?.let(WebDeepLinkInterop::decodeURIComponent)
        .orEmpty()
    return DeepLinkParts(segments, name)
}
