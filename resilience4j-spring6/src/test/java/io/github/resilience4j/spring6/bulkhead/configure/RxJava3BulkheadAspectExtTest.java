package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * aspect unit test
 */
@ExtendWith(MockitoExtension.class)
class RxJava3BulkheadAspectExtTest {

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    RxJava3BulkheadAspectExt rxJava3BulkheadAspectExt;

    @Test
    void testCheckTypes() {
        assertThat(rxJava3BulkheadAspectExt.canHandleReturnType(Flowable.class)).isTrue();
        assertThat(rxJava3BulkheadAspectExt.canHandleReturnType(Single.class)).isTrue();
    }

    @Test
    void testRxTypes() throws Throwable {
        Bulkhead bulkhead = Bulkhead.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Single.just("Test"));
        assertThat(rxJava3BulkheadAspectExt.handle(proceedingJoinPoint, bulkhead, "testMethod"))
            .isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flowable.just("Test"));
        assertThat(rxJava3BulkheadAspectExt.handle(proceedingJoinPoint, bulkhead, "testMethod"))
            .isNotNull();
    }
}
