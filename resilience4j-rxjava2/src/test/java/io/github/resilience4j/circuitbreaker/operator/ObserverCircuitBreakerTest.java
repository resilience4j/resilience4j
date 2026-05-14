package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.Observable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Unit test for {@link ObserverCircuitBreaker}.
 */
class ObserverCircuitBreakerTest {

    private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class, RETURNS_DEEP_STUBS);

    @Test
    void shouldSubscribeToObservableJust() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Observable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertResult("Event 1", "Event 2");

        then(circuitBreaker).should().onSuccess(anyLong(), any(TimeUnit.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldPropagateError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Observable.error(new IOException("BAM!"))
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        then(circuitBreaker).should()
            .onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldEmitErrorWithCallNotPermittedException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        Observable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldReleasePermissionOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Observable.just(1)
            .delay(1, TimeUnit.DAYS)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .cancel();

        then(circuitBreaker).should().releasePermission();
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
    }
}
