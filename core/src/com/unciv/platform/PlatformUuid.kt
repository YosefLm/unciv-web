package com.unciv.platform

import java.util.UUID

/** JVM UUID bit construction kept behind a web overlay. */
object PlatformUuid {
    @JvmStatic
    fun fromBits(mostSigBits: Long, leastSigBits: Long): UUID = UUID(mostSigBits, leastSigBits)
}
