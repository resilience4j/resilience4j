package io.github.resilience4j.ratelimiter.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link RateLimiterSingleObserver}.
 */
@SuppressWarnings("unchecked")
public class RateLimiterSingleObserverTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitEvent() {
        Single.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);

        assertSinglePermitUsed();
    }

    @Test
    public void shouldPropagateError() {
        Single.error(new IOException("BAM!"))
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

        Single.just(1)
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
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
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
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = RateLimiterOperator.of(rateLimiter).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, never()).onError(any());
        assertSinglePermitUsed();
    }

}
