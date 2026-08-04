package com.unciv.platform

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

/** JVM coroutine context retained behind a platform boundary for TeaVM. */
object PlatformCoroutineContext {
    @JvmStatic
    fun io(): CoroutineContext = Dispatchers.IO
}
