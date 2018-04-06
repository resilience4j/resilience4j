package io.github.resilience4j.bulkhead.operator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

/**
 * Unit test for {@link BulkheadObserver} using {@link BulkheadOperator}.
 */
@SuppressWarnings("unchecked")
public class BulkheadObserverTest {
    private Bulkhead bulkhead = Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitTime(0).build());

    @Test
    public void shouldEmitAllEvents() {
        Observable.fromArray("Event 1", "Event 2")
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult("Event 1", "Event 2");

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldPropagateError() {
        Observable.error(new IOException("BAM!"))
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

        Observable.fromArray("Event 1", "Event 2")
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnNext() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
        decoratedObserver.onSubscribe(disposable);

        // When
        decoratedObserver.onNext("one");
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onNext("two");

        // Then
        verify(childObserver, times(1)).onNext(any());
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, never()).onError(any());
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnComplete() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onComplete();

        // Then
        verify(childObserver, never()).onComplete();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldNotReleaseBulkheadWhenWasDisposedAfterNotPermittedSubscribe() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        Observer childObserver = mock(Observer.class);
        Observer decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        bulkhead.isCallPermitted();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
    }
}
