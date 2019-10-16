package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Completable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Unit test for {@link CompletableBulkhead} using {@link BulkheadOperator}.
 */
public class CompletableBulkheadTest {

    private Bulkhead bulkhead;

    @Before
    public void setUp(){
        bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void shouldComplete() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertComplete();

        then(bulkhead).should().onComplete();
    }

    @Test
    public void shouldPropagateError() {
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
    public void shouldEmitErrorWithBulkheadFullException() {
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
