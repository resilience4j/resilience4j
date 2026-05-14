package io.github.resilience4j.spring6.retry.configure;

import io.github.resilience4j.retry.Retry;
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
class RxJava3RetryAspectExtTest {

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    RxJava3RetryAspectExt rxJava3RetryAspectExt;

    @Test
    void testCheckTypes() {
        assertThat(rxJava3RetryAspectExt.canHandleReturnType(Flowable.class)).isTrue();
        assertThat(rxJava3RetryAspectExt.canHandleReturnType(Single.class)).isTrue();
    }

    @Test
    void testRxTypes() throws Throwable {
        Retry retry = Retry.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Single.just("Test"));
        assertThat(rxJava3RetryAspectExt.handle(proceedingJoinPoint, retry, "testMethod"))
            .isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flowable.just("Test"));
        assertThat(rxJava3RetryAspectExt.handle(proceedingJoinPoint, retry, "testMethod"))
            .isNotNull();
    }
}
