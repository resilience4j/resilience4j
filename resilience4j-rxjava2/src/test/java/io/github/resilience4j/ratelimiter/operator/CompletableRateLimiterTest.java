package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Completable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;

import static org.mockito.BDDMockito.given;

/**
 * Unit test for {@link CompletableRateLimiter}.
 */
public class CompletableRateLimiterTest {

    private RateLimiter rateLimiter;

    @Before
    public void setUp(){
        rateLimiter = Mockito.mock(RateLimiter.class);
    }

    @Test
    public void shouldEmitCompleted() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertComplete();
    }

    @Test
    public void shouldPropagateError() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        Completable.error(new IOException("BAM!"))
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();
    }

    @Test
    public void shouldEmitErrorWithRequestNotPermittedException() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(false);

        Completable.complete()
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();
    }
}
