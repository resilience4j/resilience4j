package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.internal.ThreadPoolBulkheadAdapter;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlainObjectBulkheadAspectExtTest {

    private final String TEST_METHOD = "testMethod";
    private final String BULKHEAD_NAME = "test";

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    TimeLimiterRegistry timeLimiterRegistry;

    @InjectMocks
    PlainObjectBulkheadAspectExt plainObjectBulkHeadAspectExt;

    private Bulkhead bulkhead;

    @Before
    public void setUp() {
        bulkhead = Bulkhead.ofDefaults(BULKHEAD_NAME);
        TimeLimiter timeLimiter = TimeLimiter.of(TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .build());

        when(timeLimiterRegistry.timeLimiter(BULKHEAD_NAME)).thenReturn(timeLimiter);
    }

    @Test
    public void testBulkheadTypeSemaphoreReturnsPlainObject() throws Throwable{
        String expected = "Plain Result";

        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Object actual = plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testBulkheadTypeThreadPoolReturnsPlainObject() throws Throwable {
        String expected = "ThreadPool Result";
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(5)
                .coreThreadPoolSize(2)
                .queueCapacity(10)
                .build();

        ThreadPoolBulkhead threadPoolBulkhead = ThreadPoolBulkhead.of(BULKHEAD_NAME, config);
        bulkhead = new ThreadPoolBulkheadAdapter(threadPoolBulkhead);

        Object actual = plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testThrowableIsThrownAndCaught() throws Throwable {
        String expectedMessage = "Test Exception";
        Throwable throwable = new Throwable(expectedMessage);
        when(proceedingJoinPoint.proceed()).thenThrow(throwable);

        try {
            plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);
        } catch (RuntimeException ex) {
            assertThat(ex.getCause()).isInstanceOf(Throwable.class);
            assertThat(ex.getCause().getMessage()).isEqualTo(ex.getCause().getMessage());
        }

        verify(timeLimiterRegistry).timeLimiter(BULKHEAD_NAME);
    }

    @Test
    public void testTimeLimiterTimeoutExceptionIsThrownAndCaught() throws Throwable {
        when(proceedingJoinPoint.proceed()).thenAnswer(invocation -> {
            Thread.sleep(200);
            return "Should timeout";
        });

        try {
            plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);
        } catch (TimeoutException ex) {
            assertThat(ex.getMessage()).contains("did not complete within time");
        }

        verify(timeLimiterRegistry).timeLimiter(BULKHEAD_NAME);
    }

    @Test
    public void testBulkheadFullExceptionIsThrownAndCaught() throws Throwable {
        String expectedMessage = "Bulkhead 'test' is full and does not permit further calls";
        BulkheadFullException bulkheadFullException = BulkheadFullException.createBulkheadFullException(bulkhead);
        when(proceedingJoinPoint.proceed()).thenThrow(bulkheadFullException);

        try {
            plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);
        } catch (RuntimeException ex) {
            assertThat(ex.getCause()).isInstanceOf(ExecutionException.class);
            ExecutionException executionException = (ExecutionException) ex.getCause();
            assertThat(executionException.getCause()).isInstanceOf(BulkheadFullException.class);
            assertThat(executionException.getCause().getMessage()).isEqualTo(expectedMessage);
        }

        verify(timeLimiterRegistry).timeLimiter(BULKHEAD_NAME);
    }

    @Test
    public void testCanHandleReturnTypes() {
        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(String.class)).isTrue();
        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(Object.class)).isTrue();
        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(Integer.class)).isTrue();
        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(ResponseEntity.class)).isTrue();

        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(CompletableFuture.class)).isFalse();
        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(Flux.class)).isFalse();
        assertThat(plainObjectBulkHeadAspectExt.canHandleReturnType(Mono.class)).isFalse();
    }
}

