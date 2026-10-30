package io.github.resilience4j.rxjava3.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link CompletableBulkhead} using {@link BulkheadOperator}.
 */
class CompletableBulkheadTest {

    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {
        bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void shouldComplete() {
        given(bulkhead.acquirePermissionAsync())
            .willReturn(CompletableFuture.completedFuture(null));

        Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertComplete();

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldPropagateError() {
        given(bulkhead.acquirePermissionAsync())
            .willReturn(CompletableFuture.completedFuture(null));

        Completable.error(new IOException("BAM!"))
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertError(IOException.class)
            .assertNotComplete();

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldEmitErrorWithBulkheadFullException() {
        CompletableFuture<Void> rejectedPermission = CompletableFuture
            .failedFuture(BulkheadFullException.createBulkheadFullException(bulkhead));
        given(bulkhead.acquirePermissionAsync()).willReturn(rejectedPermission);

        Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        then(bulkhead).should(never()).onComplete();
    }
}
