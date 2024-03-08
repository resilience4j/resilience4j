package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * aspect unit test
 */
@RunWith(MockitoJUnitRunner.class)
public class RxJava3RateLimiterAspectExtTest {

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    RxJava3RateLimiterAspectExt rxJava3RateLimiterAspectExt;


    @Test
    public void testCheckTypes() {
        assertThat(rxJava3RateLimiterAspectExt.canHandleReturnType(Flowable.class)).isTrue();
        assertThat(rxJava3RateLimiterAspectExt.canHandleReturnType(Single.class)).isTrue();
    }

    @Test
    public void testRxTypes() throws Throwable {
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