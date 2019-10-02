package io.github.resilience4j.circuitbreaker.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.Completable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

/**
 * Unit test for {@link CompletableCircuitBreaker}.
 */
public class CompletableCircuitBreakerTest extends BaseCircuitBreakerTest {

    @Test
    public void shouldSubscribeToCompletable() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Completable.complete()
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertSubscribed()
                .assertComplete();

        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldPropagateAndMarkError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Completable.error(new IOException("BAM!"))
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertSubscribed()
                .assertError(IOException.class)
                .assertNotComplete();

        verify(circuitBreaker, times(1))
                .onError(anyLong(), any(TimeUnit.class), any(IOException.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void shouldEmitErrorWithCallNotPermittedException() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);

        Completable.complete()
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertSubscribed()
                .assertError(CallNotPermittedException.class)
                .assertNotComplete();

        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }
}
