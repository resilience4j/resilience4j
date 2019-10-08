package io.github.resilience4j.kotlin.ratelimiter

import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.TimeUnit


@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.rateLimiter(rateLimiter: RateLimiter): Flow<T> =
    flow {
        val waitTimeNs = rateLimiter.reservePermission()
        if (waitTimeNs < 0) throw RequestNotPermitted.createRequestNotPermitted(rateLimiter)

        val source = this@rateLimiter.onStart {
            delay(TimeUnit.NANOSECONDS.toMillis(waitTimeNs))
        }

        emitAll(source)
    }