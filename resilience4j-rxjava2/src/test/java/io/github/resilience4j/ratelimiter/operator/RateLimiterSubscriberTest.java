package io.github.resilience4j.ratelimiter.operator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.reactivex.Flowable;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Unit test for {@link RateLimiterSubscriber}.
 */
@SuppressWarnings("unchecked")
public class RateLimiterSubscriberTest extends RateLimiterAssertions {

    @Test
    public void shouldEmitSingleEventWithSinglePermit() {
        Flowable.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1);

        assertSinglePermitUsed();
    }

    @Test
    public void shouldEmitAllEvents() {
        Flowable.fromArray(1, 2)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertResult(1, 2);

        assertUsedPermits(2);
    }

    @Test
    public void shouldPropagateError() {
        Flowable.error(new IOException("BAM!"))
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

        Flowable.just(1)
            .lift(RateLimiterOperator.of(rateLimiter))
            .test()
            .assertSubscribed()
            .assertError(RequestNotPermitted.class)
            .assertNotComplete();

        assertNoPermitLeft();
    }

    @Test
    public void shouldHonorCancelledWhenCallingOnNext() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = RateLimiterOperator.of(rateLimiter).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onNext(1);

        // Then
        verify(childSubscriber, never()).onNext(any());
        assertSinglePermitUsed();
    }

    @Test
    public void shouldHonorCancelledWhenCallingOnError() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = RateLimiterOperator.of(rateLimiter).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onError(new IllegalStateException());

        // Then
        verify(childSubscriber, never()).onError(any());
        assertSinglePermitUsed();
    }

    @Test
    public void shouldHonorCancelledWhenCallingOnComplete() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = RateLimiterOperator.of(rateLimiter).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onComplete();

        // Then
        verify(childSubscriber, never()).onComplete();
        assertSinglePermitUsed();
    }
}
