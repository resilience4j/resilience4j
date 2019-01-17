import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.utils.CircuitBreakerUtils

fun <T> decorateSuspendingFunction(circuitBreaker: CircuitBreaker, function: suspend () -> T): suspend () -> T {
    return {
        CircuitBreakerUtils.isCallPermitted(circuitBreaker)
        val start = System.nanoTime()
        try {
            val result = function()
            val durationInNanos = System.nanoTime() - start
            circuitBreaker.onSuccess(durationInNanos)
            result
        } catch (throwable: Throwable) {
            val durationInNanos = System.nanoTime() - start
            circuitBreaker.onError(durationInNanos, throwable)
            throw throwable
        }
    }
}
