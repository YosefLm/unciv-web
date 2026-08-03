package com.unciv.platform

import java.util.UUID

/** TeaVM-compatible UUID construction without the missing two-long constructor. */
object PlatformUuid {
    @JvmStatic
    fun fromBits(mostSigBits: Long, leastSigBits: Long): UUID {
        fun Long.hex(digits: Int): String {
            val alphabet = "0123456789abcdef"
            val chars = CharArray(digits)
            for (index in 0 until digits) {
                val shift = (digits - index - 1) * 4
                chars[index] = alphabet[((this ushr shift) and 0xFL).toInt()]
            }
            return String(chars)
        }
        val value = "${mostSigBits.ushr(32).hex(8)}-${mostSigBits.ushr(16).hex(4)}-${mostSigBits.hex(4)}-" +
            "${leastSigBits.ushr(48).hex(4)}-${leastSigBits.hex(12)}"
        return UUID.fromString(value)
    }
}
