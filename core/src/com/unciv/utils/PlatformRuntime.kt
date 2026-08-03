package com.unciv.utils

/** JVM packaged-resource detection kept behind a web overlay. */
fun isRunFromJar(obj: Any): Boolean = obj::class.java.`package`.specificationVersion != null
