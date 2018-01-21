package io.github.resilience4j.ratelimiter.operator;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link RateLimiterCompletableObserver}.
 */
@SuppressWarnings("unchecked")
public class RateLimiterCompletableObserverTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitCompleted() {
        Completable.complete()
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertComplete();

        assertSinglePermitUsed();
    }

    @Test
    public void shouldPropagateError() {
        Completable.error(new IOException("BAM!"))
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

        Completable.complete()
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();

        assertNoPermitLeft();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnComplete() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        CompletableObserver childObserver = mock(CompletableObserver.class);
        CompletableObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onComplete();

        // Then
        verify(childObserver, never()).onComplete();
        assertSinglePermitUsed();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        CompletableObserver childObserver = mock(CompletableObserver.class);
        CompletableObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, never()).onError(any());
        assertSinglePermitUsed();
    }
}
