package io.github.resilience4j.kotlin

import kotlinx.coroutines.Job
import java.util.concurrent.CancellationException
import kotlin.coroutines.CoroutineContext

internal fun isCancellation(error: Throwable?, coroutineContext: CoroutineContext): Boolean {

    // If job is missing then there is no cancellation
    val job = coroutineContext[Job] ?: return false

    return job.isCancelled || (error != null && error is CancellationException)
}
