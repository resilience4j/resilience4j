package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.reactivex.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RxJava2TimeLimiterAspectExtTest {
    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    RxJava2TimeLimiterAspectExt rxJava2TimeLimiterAspectExt;

    @Test
    public void testCheckTypes() {
        assertThat(rxJava2TimeLimiterAspectExt.canHandleReturnType(Flowable.class)).isTrue();
        assertThat(rxJava2TimeLimiterAspectExt.canHandleReturnType(Single.class)).isTrue();
        assertThat(rxJava2TimeLimiterAspectExt.canHandleReturnType(Observable.class)).isTrue();
        assertThat(rxJava2TimeLimiterAspectExt.canHandleReturnType(Completable.class)).isTrue();
        assertThat(rxJava2TimeLimiterAspectExt.canHandleReturnType(Maybe.class)).isTrue();
    }

    @Test
    public void testRxJava2Types() throws Throwable {
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");

        when(proceedingJoinPoint.proceed()).thenReturn(Single.just("Test"));
        assertThat(rxJava2TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Flowable.just("Test"));
        assertThat(rxJava2TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Observable.just("Test"));
        assertThat(rxJava2TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Completable.complete());
        assertThat(rxJava2TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();

        when(proceedingJoinPoint.proceed()).thenReturn(Maybe.just("Test"));
        assertThat(rxJava2TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod")).isNotNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowIllegalArgumentExceptionWithNotRxJava2Type() throws Throwable{
        TimeLimiter timeLimiter = TimeLimiter.ofDefaults("test");
        when(proceedingJoinPoint.proceed()).thenReturn("NOT RXJAVA2 TYPE");
        rxJava2TimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, "testMethod");
    }

}