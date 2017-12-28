package io.github.resilience4j.circuitbreaker.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.reactivex.Flowable;
import org.junit.Test;

/**
 * Unit test for {@link CircuitBreakerSubscriber}.
 */
public class CircuitBreakerSubscriberTest {

    @Test
    public void shouldReturnOnCompleteUsingFlowable() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        Flowable.fromArray("Event 1", "Event 2")
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertValueCount(2)
            .assertValues("Event 1", "Event 2")
            .assertComplete();

        //Then
        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReturnOnErrorUsingFlowable() {
        //Given
        CircuitBreaker circuitBreaker = CircuitBreaker.ofDefaults("testName");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        //When
        Flowable.fromCallable(() -> {throw new IOException("BAM!");})
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


    @Test
    public void shouldReturnOnErrorWithCircuitBreakerOpenExceptionFlowable() {
        // Given
        // Create a custom configuration for a CircuitBreaker
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(2)
            .ringBufferSizeInHalfOpenState(2)
            .waitDurationInOpenState(Duration.ofMillis(1000))
            .build();

        // Create a CircuitBreakerRegistry with a custom global configuration
        CircuitBreaker circuitBreaker = CircuitBreaker.of("testName", circuitBreakerConfig);
        circuitBreaker.onError(0, new RuntimeException("Bla"));
        circuitBreaker.onError(0, new RuntimeException("Bla"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        Flowable.fromArray("Event 1", "Event 2")
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(CircuitBreakerOpenException.class)
            .assertNotComplete()
            .assertSubscribed();

        CircuitBreaker.Metrics metrics = circuitBreaker.getMetrics();

        assertThat(metrics.getNumberOfBufferedCalls()).isEqualTo(2);
        assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(2);
    }

}
