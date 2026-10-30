package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link MaybeBulkhead} using {@link BulkheadOperator}.
 */
class MaybeBulkheadTest {

    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {
        bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void shouldEmitAllEvents() {
        given(bulkhead.acquirePermissionAsync())
            .willReturn(CompletableFuture.completedFuture(null));

        Maybe.just(1)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult(1);

        verify(bulkhead).onComplete();
    }

    @Test
    void shouldPropagateError() {
        given(bulkhead.acquirePermissionAsync())
            .willReturn(CompletableFuture.completedFuture(null));

        Maybe.error(new IOException("BAM!"))
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        verify(bulkhead).onComplete();
    }

    @Test
    void shouldEmitErrorWithBulkheadFullException() {
        CompletableFuture<Void> rejectedPermission = CompletableFuture
            .failedFuture(BulkheadFullException.createBulkheadFullException(bulkhead));
        given(bulkhead.acquirePermissionAsync()).willReturn(rejectedPermission);

        Maybe.just(1)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        verify(bulkhead, never()).onComplete();
    }

    @Test
    void shouldReleaseBulkheadOnlyOnce() {
        given(bulkhead.acquirePermissionAsync())
            .willReturn(CompletableFuture.completedFuture(null));

        Maybe.just(Arrays.asList(1, 2, 3))
            .compose(BulkheadOperator.of(bulkhead))
            .flatMapObservable(Observable::fromIterable)
            .take(2) //this with the previous line triggers an extra dispose
            .test()
            .assertResult(1, 2);

        verify(bulkhead).onComplete();
    }
}
