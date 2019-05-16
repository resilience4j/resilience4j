package io.github.resilience4j.ratelimiter.operator;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Flowable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Duration;

import static org.mockito.BDDMockito.given;

/**
 * Unit test for {@link FlowableRateLimiter}.
 */
public class FlowableRateLimiterTest {

    private RateLimiter rateLimiter;

    @Before
    public void setUp(){
        rateLimiter = Mockito.mock(RateLimiter.class);
    }

    @Test
    public void shouldEmitSingleEventWithSinglePermit() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        Flowable.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);
    }

    @Test
    public void shouldEmitAllEvents() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        Flowable.fromArray(1, 2)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1, 2);
    }

    @Test
    public void shouldPropagateError() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(true);

        Flowable.error(new IOException("BAM!"))
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();
    }

    @Test
    public void shouldEmitErrorWithRequestNotPermittedException() {
        given(rateLimiter.acquirePermission(Duration.ZERO)).willReturn(false);

        Flowable.just(1)
            .compose(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();
    }
}
