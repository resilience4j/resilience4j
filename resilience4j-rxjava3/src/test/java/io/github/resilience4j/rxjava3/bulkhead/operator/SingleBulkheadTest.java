package io.github.resilience4j.rxjava3.bulkhead.operator;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

/**
 * Unit test for {@link SingleBulkhead} using {@link BulkheadOperator}.
 */
class SingleBulkheadTest {

    private Bulkhead bulkhead;

    @BeforeEach
    void setUp() {
        bulkhead = mock(Bulkhead.class, RETURNS_DEEP_STUBS);
    }

    @Test
    void shouldEmitAllEvents() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Single.just(1)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertResult(1);

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldPropagateError() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Single.error(new IOException("BAM!"))
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertError(IOException.class)
            .assertNotComplete();

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldEmitErrorWithBulkheadFullException() {
        given(bulkhead.tryAcquirePermission()).willReturn(false);

        Single.just(1)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .assertError(BulkheadFullException.class)
            .assertNotComplete();

        then(bulkhead).should(never()).onComplete();
    }

    @Test
    void shouldReleaseBulkheadOnlyOnce() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Single.just(Arrays.asList(1, 2, 3))
            .compose(BulkheadOperator.of(bulkhead))
            .flatMapObservable(Observable::fromIterable)
            .take(2) //this with the previous line triggers an extra dispose
            .test()
            .assertResult(1, 2);

        then(bulkhead).should().onComplete();
    }

    @Test
    void shouldReleasePermissionOnCancel() {
        given(bulkhead.tryAcquirePermission()).willReturn(true);

        Single.just(1)
            .delay(1, TimeUnit.DAYS)
            .compose(BulkheadOperator.of(bulkhead))
            .test()
            .dispose();

        then(bulkhead).should().releasePermission();
        then(bulkhead).should(never()).onComplete();
    }
}
