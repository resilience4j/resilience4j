package io.github.resilience4j.ratelimiter;

import org.junit.Test;

import java.time.Duration;
import java.util.function.Supplier;

import static io.github.resilience4j.core.ResultUtils.isFailedAndThrown;
import static io.github.resilience4j.core.ResultUtils.isSuccessfulAndReturned;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class RateLimiterWithConditionalDrainIntegrationTest {

    @Test
    public void shouldDrainRateLimiterInConditionMetOnFailedCall() {
        RateLimiter limiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isFailedAndThrown(callsResult, SubsystemOverloadException.class))
            .build());
        Runnable call = () -> {
            throw new SpecificSubsystemOverloadException();
        };
        Runnable decoratedCall = RateLimiter.decorateRunnable(limiter, call);

        Throwable thrown = catchThrowable(decoratedCall::run);

        assertThat(thrown).isInstanceOf(SubsystemOverloadException.class);
        assertThat(limiter.getMetrics().getAvailablePermissions()).isZero();
    }

    @Test
    public void shouldDrainRateLimiterInConditionMetOnSuccessfulCall() {
        RateLimiter limiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isSuccessfulAndReturned(
                    callsResult,
                    ResponseWithOverloadIndication.class,
                    ResponseWithOverloadIndication::isPotentialOverload))
            .build());
        Supplier<Object> call = () -> new SpecificResponseWithOverloadIndication(true);
        Supplier<Object> decoratedCall = RateLimiter.decorateSupplier(limiter, call);

        Object result = decoratedCall.get();

        assertThat(result).isInstanceOf(ResponseWithOverloadIndication.class);
        assertThat(limiter.getMetrics().getAvailablePermissions()).isZero();
    }

    @Test
    public void shouldNotDrainRateLimiterInConditionNotMetOnSuccessfulCall() {
        RateLimiter limiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isSuccessfulAndReturned(
                    callsResult,
                    ResponseWithOverloadIndication.class,
                    ResponseWithOverloadIndication::isPotentialOverload))
            .build());
        Supplier<Object> call = () -> new SpecificResponseWithOverloadIndication(false);
        Supplier<Object> decoratedCall = RateLimiter.decorateSupplier(limiter, call);

        Object result = decoratedCall.get();

        assertThat(result).isInstanceOf(ResponseWithOverloadIndication.class);
        assertThat(limiter.getMetrics().getAvailablePermissions()).isEqualTo(4);
    }

    private static class SubsystemOverloadException extends RuntimeException { }

    private static class SpecificSubsystemOverloadException extends SubsystemOverloadException { }

    private static class ResponseWithOverloadIndication {
        private final boolean potentialOverload;

        public ResponseWithOverloadIndication(boolean potentialOverload) {
            this.potentialOverload = potentialOverload;
        }

        public boolean isPotentialOverload() {
            return potentialOverload;
        }
    }

    private static class SpecificResponseWithOverloadIndication extends ResponseWithOverloadIndication {

        public SpecificResponseWithOverloadIndication(boolean potentialOverload) {
            super(potentialOverload);
        }
    }
}
