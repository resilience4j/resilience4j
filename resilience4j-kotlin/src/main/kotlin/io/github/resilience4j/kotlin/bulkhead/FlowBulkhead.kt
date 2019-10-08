package io.github.resilience4j.kotlin.bulkhead

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.kotlin.isCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlin.coroutines.coroutineContext

@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.bulkhead(bulkhead: Bulkhead): Flow<T> =
    flow {
        bulkhead.acquirePermissionSuspend()

        val source = this@bulkhead.onCompletion { e ->
            if(isCancellation(coroutineContext, e)){
                bulkhead.releasePermission()
            }else{
                bulkhead.onComplete()
            }
        }
        emitAll(source)
    }