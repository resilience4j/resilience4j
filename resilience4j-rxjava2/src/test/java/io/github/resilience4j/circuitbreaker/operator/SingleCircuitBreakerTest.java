package io.github.resilience4j.circuitbreaker.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.Single;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit test for {@link SingleCircuitBreaker}.
 */
public class SingleCircuitBreakerTest extends BaseCircuitBreakerTest {

    @Test
    public void shouldSubscribeToSingleJust() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Single.just(1)
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertResult(1);

        verify(circuitBreaker, times(1)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldSubscribeToMonoFromCallableMultipleTimes() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        Single.fromCallable(() -> helloWorldService.returnHelloWorld())
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .repeat(2)
                .test()
                .assertResult("Hello World", "Hello World");

        then(helloWorldService).should(Mockito.times(2)).returnHelloWorld();
        verify(circuitBreaker, times(2)).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldNotSubscribeToSingleFromCallable() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        Single.fromCallable(() -> helloWorldService.returnHelloWorld())
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertSubscribed()
                .assertError(CallNotPermittedException.class)
                .assertNotComplete();

        then(helloWorldService).should(never()).returnHelloWorld();
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldPropagateError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Single.error(new IOException("BAM!"))
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

        Single.just(1)
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .assertSubscribed()
                .assertError(CallNotPermittedException.class)
                .assertNotComplete();

        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    public void shouldReleasePermissionOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Single.just(1)
                .delay(1, TimeUnit.DAYS)
                .compose(CircuitBreakerOperator.of(circuitBreaker))
                .test()
                .cancel();

        verify(circuitBreaker, times(1)).releasePermission();
        verify(circuitBreaker, never())
                .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        verify(circuitBreaker, never()).onSuccess(anyLong(), any(TimeUnit.class));
    }
}
