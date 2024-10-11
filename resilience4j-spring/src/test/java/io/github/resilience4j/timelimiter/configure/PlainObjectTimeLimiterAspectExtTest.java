package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.timelimiter.TimeLimiter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlainObjectTimeLimiterAspectExtTest {

    private final String TEST_METHOD = "testMethod";

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    TimeLimiter timeLimiter;

    @InjectMocks
    PlainObjectTimeLimiterAspectExt plainObjectTimeLimiterAspectExt;

    @Test
    public void testCheckTypes() {
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(String.class)).isTrue();
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(Object.class)).isTrue();
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(Integer.class)).isTrue();
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(ResponseEntity.class)).isTrue();
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(CompletableFuture.class)).isFalse();
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(Flux.class)).isFalse();
        assertThat(plainObjectTimeLimiterAspectExt.canHandleReturnType(Mono.class)).isFalse();
    }

    @Test(expected = TimeoutException.class)
    public void testHandleThrowsTimeoutException() throws Throwable {
        when(proceedingJoinPoint.proceed()).thenReturn("result");
        when(timeLimiter.executeFutureSupplier(any(Supplier.class))).thenThrow(new TimeoutException());
        plainObjectTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, TEST_METHOD);
    }

    @Test
    public void testHandleReturnsPlainObject() throws Throwable {
        String expected = "Plain Result";

        when(proceedingJoinPoint.proceed()).thenReturn(expected);
        when(timeLimiter.executeFutureSupplier(any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<Object> supplier = invocation.getArgument(0);
                    return supplier.get();
                });

        Object actual = plainObjectTimeLimiterAspectExt.handle(proceedingJoinPoint, timeLimiter, TEST_METHOD);

        assertThat(actual).isEqualTo(expected);
    }
}
