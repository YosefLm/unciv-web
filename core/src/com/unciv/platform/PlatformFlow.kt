package com.unciv.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlin.coroutines.CoroutineContext

/** JVM flow dispatch retained behind a platform boundary for TeaVM. */
fun <T> Flow<T>.platformFlowOn(context: CoroutineContext): Flow<T> = flowOn(context)
