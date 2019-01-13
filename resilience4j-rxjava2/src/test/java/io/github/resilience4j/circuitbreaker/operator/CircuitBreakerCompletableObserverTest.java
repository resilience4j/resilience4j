package io.github.resilience4j.circuitbreaker.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link CircuitBreakerCompletableObserver}.
 */
public class CircuitBreakerCompletableObserverTest extends CircuitBreakerAssertions {

    @Test
    public void shouldCompleteAndMarkSuccess() {
        Completable.complete()
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertComplete();

        assertSingleSuccessfulCall();
    }

    @Test
    public void shouldPropagateAndMarkError() {
        Completable.error(new IOException("BAM!"))
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        assertSingleFailedCall();
    }

    @Test
    public void shouldEmitErrorWithCircuitBreakerOpenException() {
        circuitBreaker.transitionToOpenState();

        Completable.complete()
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(CircuitBreakerOpenException.class)
            .assertNotComplete();

        assertNoRegisteredCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnComplete() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        CompletableObserver childObserver = mock(CompletableObserver.class);
        CompletableObserver decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onComplete();

        // Then
        verify(childObserver, never()).onComplete();
        assertNoRegisteredCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        CompletableObserver childObserver = mock(CompletableObserver.class);
        CompletableObserver decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, never()).onError(any());
        assertNoRegisteredCall();
    }

    @Test
    public void shouldNotAffectCircuitBreakerWhenWasDisposedAfterNotPermittedSubscribe() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        CompletableObserver childObserver = mock(CompletableObserver.class);
        CompletableObserver decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
        circuitBreaker.transitionToOpenState();
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();

        // Then
        assertNoRegisteredCall();
    }
}
