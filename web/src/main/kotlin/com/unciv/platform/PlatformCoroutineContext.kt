package com.unciv.platform

import com.unciv.utils.Dispatcher
import kotlin.coroutines.CoroutineContext

/** Web overlay: no JVM scheduler is reachable from an IO flow. */
object PlatformCoroutineContext {
    @JvmStatic
    fun io(): CoroutineContext = Dispatcher.DAEMON
}
