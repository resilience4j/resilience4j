package io.github.resilience4j.kotlin.timelimiter

import io.github.resilience4j.timelimiter.TimeLimiter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow


@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.timeLimiter(timeLimiter: TimeLimiter): Flow<T> {
    val source = this
    return flow {
        timeLimiter.executeSuspendFunction {
            emitAll(source)
        }
    }
}