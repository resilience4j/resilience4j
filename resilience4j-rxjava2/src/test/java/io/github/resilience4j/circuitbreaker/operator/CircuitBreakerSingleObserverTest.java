package io.github.resilience4j.circuitbreaker.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Single;
import org.junit.Test;

/**
 * Unit test for {@link CircuitBreakerSingleObserver}.
 */
public class CircuitBreakerSingleObserverTest {
    @Test
    public void shouldReturnOnCompleteUsingSingle() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Single.just(1)
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertValueCount(1)
            .assertValues(1)
            .assertComplete();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingUsingSingle() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        Single.fromCallable(() -> {throw new IOException("BAM!");})
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(IOException.class)
            .assertNotComplete()
            .assertSubscribed();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
    }

}
