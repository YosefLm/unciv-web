package com.unciv.platform

import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

/** Web overlay: flows already execute on the direct dispatcher. */
fun <T> Flow<T>.platformFlowOn(context: CoroutineContext): Flow<T> = this
