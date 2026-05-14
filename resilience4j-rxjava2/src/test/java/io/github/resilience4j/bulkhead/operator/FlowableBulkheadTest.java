package io.github.resilience4j.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.Flowable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link FlowableBulkhead} using {@link BulkheadOperator}.
 */
class FlowableBulkheadTest {

    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {
        bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void shouldEmitAllEvents() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Flowable.fromArray("Event 1", "Event 2")
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult("Event 1", "Event 2");

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldPropagateError() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Flowable.error(new IOException("BAM!"))
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

        Flowable.fromArray("Event 1", "Event 2")
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertSubscribed()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        then(bulkhead).should(never()).onComplete();
    }
}
