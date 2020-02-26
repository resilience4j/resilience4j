package io.github.resilience4j.rxjava3.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.rxjava3.ratelimiter.operator.CompletableRateLimiter;
import io.github.resilience4j.rxjava3.ratelimiter.operator.RateLimiterOperator;
import io.reactivex.rxjava3.core.Completable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link CompletableRateLimiter}.
 */
public class CompletableRateLimiterTest {

    private RateLimiter rateLimiter;

    @Before
    public void setUp() {
        rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void shouldEmitCompleted() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertComplete();
    }

    @Test
    public void shouldDelaySubscription() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(1).toNanos());

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .awaitDone(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldPropagateError() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Completable.error(new IOException("BAM!"))
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertError(IOException.class)
            .assertNotComplete();
    }

    @Test
    public void shouldEmitErrorWithRequestNotPermittedException() {
        given(rateLimiter.reservePermission()).willReturn(-1L);

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();
    }
}
