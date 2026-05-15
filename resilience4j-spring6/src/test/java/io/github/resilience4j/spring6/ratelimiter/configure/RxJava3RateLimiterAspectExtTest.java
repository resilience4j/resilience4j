package io.github.resilience4j.spring6.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.rxjava3.core.*;
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
class RxJava3RateLimiterAspectExtTest {

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    RxJava3RateLimiterAspectExt rxJava3RateLimiterAspectExt;

    @Test
    void testCheckTypes() {
        assertThat(rxJava3RateLimiterAspectExt.canHandleReturnType(Flowable.class)).isTrue();
        assertThat(rxJava3RateLimiterAspectExt.canHandleReturnType(Single.class)).isTrue();
    }

    @Test
    void testRxTypes() throws Throwable {
        RateLimiter rateLimiter = RateLimiter.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Single.just("Test"));
        assertThat(
            rxJava3RateLimiterAspectExt.handle(proceedingJoinPoint, rateLimiter, "testMethod"))
            .isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flowable.just("Test"));
        assertThat(
            rxJava3RateLimiterAspectExt.handle(proceedingJoinPoint, rateLimiter, "testMethod"))
            .isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Completable.complete());
        assertThat(
            rxJava3RateLimiterAspectExt.handle(proceedingJoinPoint, rateLimiter, "testMethod"))
            .isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Maybe.just("Test"));
        assertThat(
            rxJava3RateLimiterAspectExt.handle(proceedingJoinPoint, rateLimiter, "testMethod"))
            .isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Observable.just("Test"));
        assertThat(
            rxJava3RateLimiterAspectExt.handle(proceedingJoinPoint, rateLimiter, "testMethod"))
            .isNotNull();

    }
}
