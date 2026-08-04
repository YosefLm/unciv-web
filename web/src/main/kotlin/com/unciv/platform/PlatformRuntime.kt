package com.unciv.platform

/** TeaVM has no JVM management beans. */
object PlatformRuntime {
    @JvmStatic
    fun gcCount(): Int = 0
}
