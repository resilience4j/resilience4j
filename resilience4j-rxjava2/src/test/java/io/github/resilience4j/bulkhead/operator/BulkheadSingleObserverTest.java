package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link BulkheadSingleObserver} using {@link BulkheadOperator}.
 */
@SuppressWarnings("unchecked")
public class BulkheadSingleObserverTest {
    private Bulkhead bulkhead = Bulkhead.of("test", BulkheadConfig.custom().maxConcurrentCalls(1).maxWaitTime(0).build());

    @Test
    public void shouldEmitAllEvents() {
        Single.just(1)
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult(1);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldPropagateError() {
        Single.error(new IOException("BAM!"))
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldEmitErrorWithBulkheadFullException() {
        bulkhead.obtainPermission();

        Single.just(1)
            .lift(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnSuccess() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onSuccess(1);

        // Then
        verify(childObserver, never()).onSuccess(any());
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldHonorDisposedWhenCallingOnError() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();
        decoratedObserver.onError(new IllegalStateException());

        // Then
        verify(childObserver, never()).onError(any());
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }

    @Test
    public void shouldNotReleaseBulkheadWhenWasDisposedAfterNotPermittedSubscribe() throws Exception {
        // Given
        Disposable disposable = mock(Disposable.class);
        SingleObserver childObserver = mock(SingleObserver.class);
        SingleObserver decoratedObserver = BulkheadOperator.of(bulkhead).apply(childObserver);
        bulkhead.obtainPermission();
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
        decoratedObserver.onSubscribe(disposable);

        // When
        ((Disposable) decoratedObserver).dispose();

        // Then
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(0);
    }

    @Test
    public void shouldReleaseBulkheadOnlyOnce() {
        Single.just(Arrays.asList(1, 2, 3))
            .lift(BulkheadOperator.of(bulkhead))
            .flatMapObservable(Observable::fromIterable)
            .take(2) //this with the previous line triggers an extra dispose
            .test()
            .assertResult(1, 2);

        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(1);
    }
}
