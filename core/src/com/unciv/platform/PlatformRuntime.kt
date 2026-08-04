package com.unciv.platform

import java.lang.management.ManagementFactory

/** JVM runtime facilities kept behind a web overlay. */
object PlatformRuntime {
    @JvmStatic
    fun gcCount(): Int = ManagementFactory.getGarbageCollectorMXBeans().sumOf { it.collectionCount }.toInt()
}
