package com.unciv.utils

/** Web assets are served individually rather than loaded from a JVM jar. */
fun isRunFromJar(obj: Any): Boolean = false
