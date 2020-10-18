package io.github.resilience4j.rxjava3.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.rxjava3.core.Observable;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit test for {@link ObserverCircuitBreaker}.
 */
public class ObserverCircuitBreakerTest extends BaseCircuitBreakerTest {

    @Test
    public void shouldSubscribeToObservableJust() {
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
    public void shouldPropagateError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Observable.error(new IOException("BAM!"))
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(IOException.class)
            .assertNotComplete();

        then(circuitBreaker).should()
            .onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitErrorWithCallNotPermittedException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        Observable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldReleasePermissionOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Observable.just(1)
            .delay(1, TimeUnit.DAYS)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .dispose();

        then(circuitBreaker).should().releasePermission();
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        then(circuitBreaker).should(never()).onSuccess(anyLong(), any(TimeUnit.class));
    }
}
