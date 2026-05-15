package io.github.resilience4j.rxjava3.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.test.HelloWorldService;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link FlowableCircuitBreaker}.
 */
class FlowableCircuitBreakerTest {

    private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class, RETURNS_DEEP_STUBS);

    @Test
    void shouldInvokeOnSuccess() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Flowable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertResult("Event 1", "Event 2");

        then(circuitBreaker).should().onSuccess(anyLong(), any(TimeUnit.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldInvokeOnError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Flowable.error(new IOException("BAM!"))
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(IOException.class)
            .assertNotComplete();

        then(circuitBreaker).should()
            .onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldEmitErrorWithCallNotPermittedException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        Flowable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldInvokeReleasePermissionReleaseOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Flowable.just(1)
            .delay(1, TimeUnit.DAYS)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .cancel();

        then(circuitBreaker).should().releasePermission();
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldInvokeOnSuccessOnCancelWhenOneEventWasEmitted() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Flowable.just(1, 2, 3)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test(1)
            .cancel();

        then(circuitBreaker).should(never()).releasePermission();
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        then(circuitBreaker).should().onSuccess(anyLong(), any(TimeUnit.class));
    }
}
