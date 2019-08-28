package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.Flowable;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link FlowableCircuitBreaker}.
 */
public class FlowableCircuitBreakerTest extends BaseCircuitBreakerTest {

    @Test
    public void shouldInvokeOnSuccess() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Flowable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertResult("Event 1", "Event 2");

        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldInvokeOnError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Flowable.error(new IOException("BAM!"))
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        verify(circuitBreaker, times(1)).onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitErrorWithCallNotPermittedException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        Flowable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldInvokeReleasePermissionReleaseOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Flowable.just(1)
                .delay(1, TimeUnit.DAYS)
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .cancel();

        verify(circuitBreaker, times(1)).releasePermission();
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldInvokeOnSuccessOnCancelWhenOneEventWasEmitted() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Flowable.just(1,2,3)
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test(1)
                .cancel();

        verify(circuitBreaker, never()).releasePermission();
        verify(circuitBreaker, never()).onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
    }
}
