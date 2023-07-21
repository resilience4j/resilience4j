package io.github.resilience4j.bulkhead.adaptive.internal;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead;
import io.vavr.control.Either;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig.custom;
import static org.assertj.core.api.Assertions.assertThat;

public class AdaptiveBulkheadThresholdResultHandlingTest {

    @Test
    public void shouldRecordSpecificStringResultAsAFailureAndAnyOtherAsSuccess() {
        AdaptiveBulkhead circuitBreaker = new AdaptiveBulkheadStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordResult(result -> result.equals("failure"))
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, "success");

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, "failure");

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

    @Test
    public void shouldRecordSpecificComplexResultAsAFailureAndAnyOtherAsSuccess() {
        AdaptiveBulkhead circuitBreaker = new AdaptiveBulkheadStateMachine("testName", custom()
            .slidingWindowSize(5)
            .recordResult(result ->
                result instanceof Either && ((Either<?, ?>) result).isLeft() && ((Either<?, ?>) result).getLeft().equals("failure")
            )
            .build());

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, Either.left("accepted fail"));

        // Call 2 is a failure
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        circuitBreaker.onResult(0, Either.left("failure"));

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(2);
    }

}