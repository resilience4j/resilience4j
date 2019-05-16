package io.github.resilience4j.ratelimiter.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link RateLimiterObserver}.
 */
@SuppressWarnings("unchecked")
public class RateLimiterObserverTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitSingleEventWithSinglePermit() {
        Observable.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);

        assertSinglePermitUsed();
    }

    @Test
    public void shouldEmitAllEvents() {
        Observable.fromArray(1, 2)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1, 2);

        assertUsedPermits(2);
    }

    @Test
    public void shouldPropagateError() {
        Observable.error(new IOException("BAM!"))
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        assertSinglePermitUsed();
    }

    @Test
    public void shouldEmitErrorWithRequestNotPermittedException() {
        saturateRateLimiter();

        Observable.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();

        assertNoPermitLeft();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnNext() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onNext(1);

        // Then
        verify(childObserver, never()).onNext(any());
        assertSinglePermitUsed();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, never()).onError(any());
        assertSinglePermitUsed();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnComplete() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onComplete();

        // Then
        verify(childObserver, never()).onComplete();
        assertSinglePermitUsed();
    }
}
