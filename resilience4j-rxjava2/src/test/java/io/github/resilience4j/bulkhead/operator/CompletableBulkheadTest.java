package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Completable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link CompletableBulkhead} using {@link BulkheadOperator}.
 */
public class CompletableBulkheadTest {

    private Bulkhead bulkhead;

    @Before
    public void setUp(){
        bulkhead = Mockito.mock(Bulkhead.class, RETURNS_DEEP_STUBS);
    }

    @Test
    public void shouldComplete() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Completable.complete()
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertComplete();

        verify(bulkhead, times(1)).onComplete();
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

        verify(bulkhead, times(1)).onComplete();
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

        verify(bulkhead, never()).onComplete();
    }
}
