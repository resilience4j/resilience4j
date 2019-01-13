package io.github.resilience4j.circuitbreaker.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link CircuitBreakerObserver}.
 */
@SuppressWarnings("unchecked")
public class CircuitBreakerObserverTest extends CircuitBreakerAssertions {
    @Test
    public void shouldEmitAllEvents() {
        Observable.fromArray("Event 1", "Event 2")
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertResult("Event 1", "Event 2");

        assertSingleSuccessfulCall();
    }

    @Test
    public void shouldPropagateError() {
        Observable.error(new IOException("BAM!"))
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

        Observable.fromArray("Event 1", "Event 2")
            .lift(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(CircuitBreakerOpenException.class)
            .assertNotComplete();

        assertNoRegisteredCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnNext() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        decoratedObserver.onNext("one");
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onNext("two");

        // Then
        verify(childObserver, times(1)).onNext("one");
        assertNoRegisteredCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnComplete() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
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
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
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
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
        circuitBreaker.transitionToOpenState();
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();

        // Then
        assertNoRegisteredCall();
    }
}
