package io.github.resilience4j.ratelimiter.operator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Maybe;
import io.reactivex.MaybeObserver;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link RateLimiterMaybeObserver}.
 */
@SuppressWarnings("unchecked")
public class RateLimiterMaybeObserverTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitEvent() {
        Maybe.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);

        assertSinglePermitUsed();
    }

    @Test
    public void shouldPropagateError() {
        Maybe.error(new IOException("BAM!"))
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

        Maybe.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();

        assertNoPermitLeft();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnSuccess() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        MaybeObserver childObserver = mock(MaybeObserver.class);
        MaybeObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onSuccess(1);

        // Then
        verify(childObserver, never()).onSuccess(any());
        assertSinglePermitUsed();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        MaybeObserver childObserver = mock(MaybeObserver.class);
        MaybeObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
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
        MaybeObserver childObserver = mock(MaybeObserver.class);
        MaybeObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onComplete();

        // Then
        verify(childObserver, never()).onComplete();
        assertSinglePermitUsed();
    }
}
