package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * Unit test for {@link SingleRateLimiter}.
 */
class SingleRateLimiterTest {

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = mock(RateLimiter.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void shouldEmitEvent() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Single.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);
    }

    @Test
    void shouldDelaySubscription() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(1).toNanos());

        Single.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .awaitTerminalEvent(2, TimeUnit.SECONDS);
    }

    @Test
    void shouldPropagateError() {
        given(rateLimiter.reservePermission()).willReturn(Duration.ofSeconds(0).toNanos());

        Single.error(new IOException("BAM!"))
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();
    }

    @Test
    void shouldEmitErrorWithRequestNotPermittedException() {
        given(rateLimiter.reservePermission()).willReturn(-1L);

        Single.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();
    }

}
