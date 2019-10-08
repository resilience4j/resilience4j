package io.github.resilience4j.kotlin.retry

import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*


@UseExperimental(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.retry(retry: Retry): Flow<T> {

    val retryContext = retry.asyncContext<T>()

    return onEach {
        val delayMs = retryContext.onResult(it)
        if (delayMs > 0) {
            delay(delayMs)
            throw RetryDueToResultException()
        }
    }.retryWhen { e,  _ ->

        var shouldRetry = false

        if(e is RetryDueToResultException) {
            shouldRetry = true
        } else {
            val delayMs = retryContext.onError(e)
            if(delayMs > 0) {
                delay(delayMs)
                shouldRetry = true
            }
        }

        shouldRetry

    }.onCompletion { e ->
        if(e == null)
            retryContext.onSuccess()
    }

}

private class RetryDueToResultException : RuntimeException("Retry due to retryOnResult predicate")