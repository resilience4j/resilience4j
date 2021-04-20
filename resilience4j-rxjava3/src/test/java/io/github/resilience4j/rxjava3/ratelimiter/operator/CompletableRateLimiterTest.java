package io.github.resilience4j.rxjava3.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.rxjava3.core.Completable;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.github.resilience4j.core.ResultUtils.isFailedAndThrown;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link CompletableRateLimiter}.
 */
public class CompletableRateLimiterTest {

    @Test
    public void shouldEmitCompleted() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertComplete();
    }

    @Test
    public void shouldDelaySubscription() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(1).toNanos());

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .awaitDone(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldPropagateError() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Completable.error(new IOException("BAM!"))
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertError(IOException.class)
            .assertNotComplete();
    }

    @Test
    public void shouldEmitErrorWithRequestNotPermittedException() {
        RateLimiter rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
        given(rateLimiter.reservePermission()).willReturn(-1L);

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();
    }

    @Test
    public void shouldDrainRateLimiterInConditionMetOnFailedCall() {
        RateLimiter rateLimiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(
                callsResult -> isFailedAndThrown(callsResult, OverloadException.class))
            .build());

        Completable.error(new OverloadException.SpecificOverloadException())
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertError(OverloadException.SpecificOverloadException.class)
            .awaitDone(1, TimeUnit.SECONDS);
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isZero();
    }

    @Test
    public void shouldNotDrainRateLimiterOnCompletion() {
        RateLimiter rateLimiter = RateLimiter.of("someLimiter", RateLimiterConfig.custom()
            .limitForPeriod(5)
            .limitRefreshPeriod(Duration.ofHours(1))
            .drainPermissionsOnResult(callsResult -> true)
            .build());

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertComplete();
        assertThat(rateLimiter.getMetrics().getAvailablePermissions()).isEqualTo(4);
    }
}
