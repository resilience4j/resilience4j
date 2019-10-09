/*
 *
 *  Copyright 2019 authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
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
        if (delayMs >= 0) {
            delay(delayMs)
            throw RetryDueToResultException()
        }
    }.retryWhen { e,  _ ->

        var shouldRetry = false

        if(e is RetryDueToResultException) {
            shouldRetry = true
        } else {
            val delayMs = retryContext.onError(e)
            if(delayMs >= 0) {
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