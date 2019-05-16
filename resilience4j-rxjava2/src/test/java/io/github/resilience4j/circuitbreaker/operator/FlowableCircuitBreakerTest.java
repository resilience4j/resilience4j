package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.reactivex.Flowable;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link CircuitBreakerSubscriber}.
 */
@SuppressWarnings("unchecked")
public class CircuitBreakerSubscriberTest extends CircuitBreakerAssertions {

    @Test
    public void shouldEmitAllEvents() {
        Flowable.just("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertResult("Event 1", "Event 2");

        assertSingleSuccessfulCall();
    }

    @Test
    public void shouldPropagateError() {
        Flowable.error(new IOException("BAM!"))
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        assertSingleFailedCall();
    }

    @Test
    public void shouldEmitErrorWithCallNotPermittedException() {
        circuitBreaker.transitionToOpenState();

        Flowable.fromArray("Event 1", "Event 2")
            .compose(CircuitBreakerOperator.of(circuitBreaker))
            .test()
            .assertSubscribed()
            .assertError(CallNotPermittedException.class)
            .assertNotComplete();

        assertNoRegisteredCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnNext() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = CircuitBreakerOperator.of(circuitBreaker).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        decoratedSubscriber.onNext("one");
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onNext("two");

        // Then
        verify(childSubscriber, times(1)).onNext(any());
        assertNoRegisteredCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnComplete() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = CircuitBreakerOperator.of(circuitBreaker).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onComplete();

        // Then
        verify(childSubscriber, never()).onComplete();
        assertSingleSuccessfulCall();
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = CircuitBreakerOperator.of(circuitBreaker).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onError(new IllegalStateException());

        // Then
        verify(childSubscriber, never()).onError(any());
        assertSingleFailedCall();
    }

    @Test
    public void shouldNotAffectCircuitBreakerWhenWasCancelledAfterNotPermittedSubscribe() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childObserver = mock(Subscriber.class);
        Subscriber decoratedObserver = CircuitBreakerOperator.of(circuitBreaker).apply(childObserver);
        circuitBreaker.transitionToOpenState();
        decoratedObserver.onSubscribe(subscription);

        // When
        ((Subscription) decoratedObserver).cancel();

        // Then
        assertNoRegisteredCall();
    }
}
