package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Completable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertComplete();

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldPropagateError() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Completable.error(new IOException("BAM!"))
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(IOException.class)
            .assertNotComplete();

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldEmitErrorWithBulkheadFullException() {
        given(bulkhead.tryAcquirePermission()).willReturn(false);

        Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        then(bulkhead).should(never()).onComplete();
    }
}
