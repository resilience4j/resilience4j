package io.github.resilience4j.rxjava3.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.test.HelloWorldService;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link SingleCircuitBreaker}.
 */
class SingleCircuitBreakerTest {

    private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class, RETURNS_DEEP_STUBS);
    private final HelloWorldService helloWorldService = mock(HelloWorldService.class);

    @Test
    void shouldSubscribeToSingleJust() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Single.just(1)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertResult(1);

        then(circuitBreaker).should().onResult(anyLong(), any(TimeUnit.class), any(Integer.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldSubscribeToMonoFromCallableMultipleTimes() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Single.fromCallable(() -> helloWorldService.returnHelloWorld())
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .repeat(2)
            .test()
            .assertResult("Hello World", "Hello World");

        then(helloWorldService).should(times(2)).returnHelloWorld();
        then(circuitBreaker).should(times(2)).onResult(anyLong(), any(TimeUnit.class), any(String.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldNotSubscribeToSingleFromCallable() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(false);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello World");

        Single.fromCallable(() -> helloWorldService.returnHelloWorld())
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        then(helloWorldService).should(never()).returnHelloWorld();
        then(circuitBreaker).should(never()).onResult(anyLong(), any(TimeUnit.class), any(String.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldPropagateError() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);
        given(circuitBreaker.getCurrentTimestamp()).willReturn(System.nanoTime());
        given(circuitBreaker.getTimestampUnit()).willReturn(TimeUnit.NANOSECONDS);

        Single.error(new IOException("BAM!"))
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

        Single.just(1)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        then(circuitBreaker).should(never()).onResult(anyLong(), any(TimeUnit.class), any(Integer.class));
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
    }

    @Test
    void shouldReleasePermissionOnCancel() {
        given(circuitBreaker.tryAcquirePermission()).willReturn(true);

        Single.just(1)
            .delay(1, TimeUnit.DAYS)
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .dispose();

        then(circuitBreaker).should().releasePermission();
        then(circuitBreaker).should(never())
            .onError(anyLong(), any(TimeUnit.class), any(Throwable.class));
        then(circuitBreaker).should(never()).onResult(anyLong(), any(TimeUnit.class), any(Integer.class));
    }
}
