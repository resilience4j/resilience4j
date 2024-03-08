package io.github.resilience4j.spring6.timelimiter.configure;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.reactivex.rxjava3.core.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RxJava3TimeLimiterAspectExtTest {
    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    RxJava3TimeLimiterAspectExt rxJava3TimeLimiterAspectExt;

    @Test
    public void testCheckTypes() {
        assertThat(rxJava3TimeLimiterAspectExt.canHandleReturnType(Flowable.class)).isTrue();
        assertThat(rxJava3TimeLimiterAspectExt.canHandleReturnType(Single.class)).isTrue();
        assertThat(rxJava3TimeLimiterAspectExt.canHandleReturnType(Observable.class)).isTrue();
        assertThat(rxJava3TimeLimiterAspectExt.canHandleReturnType(Completable.class)).isTrue();
        assertThat(rxJava3TimeLimiterAspectExt.canHandleReturnType(Maybe.class)).isTrue();
    }

    @Test
    public void testRxJava3Types() throws Throwable {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Single.just("Test"));
        assertThat(rxJava3TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flowable.just("Test"));
        assertThat(rxJava3TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Observable.just("Test"));
        assertThat(rxJava3TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Completable.complete());
        assertThat(rxJava3TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Maybe.just("Test"));
        assertThat(rxJava3TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWithNotRxJava3Type() throws Throwable{
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");
        when(proceedingJoinPoint.proceed()).thenReturn("NOT RXJAVA3 TYPE");

        try {
            rxJava3TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod");
            fail("exception missed");
        } catch (Throwable e) {
            assertThat(e).isInstanceOf(IllegalReturnTypeException.class)
                .hasMessage(
                    "java.lang.String testMethod has unsupported by @TimeLimiter return type. RxJava3 expects Flowable/Single/...");
        }
    }

}