package io.github.resilience4j.bulkhead.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Flowable;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Unit test for {@link BulkheadSubscriber} using {@link BulkheadOperator}.
 */
@SuppressWarnings("unchecked")
public class BulkheadSubscriberTest {
    private Bulkhead bulkhead = Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitTime(0).build());

    @Test
    public void shouldEmitAllEvents() {
        Flowable.fromArray("Event 1", "Event 2")
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult("Event 1", "Event 2");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldPropagateError() {
        Flowable.error(new IOException("BAM!"))
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldEmitErrorWithBulkheadFullException() {
        bulkhead.isCallPermitted();

        Flowable.fromArray("Event 1", "Event 2")
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
    }

    @Test
    public void shouldHonorCancelledWhenCallingOnNext() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = BulkheadOperator.of(bulkhead).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        decoratedSubscriber.onNext("one");
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onNext("two");

        // Then
        verify(childSubscriber, times(1)).onNext(any());
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldHonorCancelledWhenCallingOnError() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = BulkheadOperator.of(bulkhead).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onError(new IllegalStateException());

        // Then
        verify(childSubscriber, never()).onError(any());
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldHonorCancelledWhenCallingOnComplete() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childSubscriber = mock(Subscriber.class);
        Subscriber decoratedSubscriber = BulkheadOperator.of(bulkhead).apply(childSubscriber);
        decoratedSubscriber.onSubscribe(subscription);

        // When
        ((Subscription) decoratedSubscriber).cancel();
        decoratedSubscriber.onComplete();

        // Then
        verify(childSubscriber, never()).onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldNotReleaseBulkheadWhenWasCancelledAfterNotPermittedSubscribe() throws Exception {
        // Given
        Subscription subscription = mock(Subscription.class);
        Subscriber childObserver = mock(Subscriber.class);
        Subscriber decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        bulkhead.isCallPermitted();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
        decoratedObserver.onSubscribe(subscription);

        // When
        ((Subscription) decoratedObserver).cancel();

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
    }
}
