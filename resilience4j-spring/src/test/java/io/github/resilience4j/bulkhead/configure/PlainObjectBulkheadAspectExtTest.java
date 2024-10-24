package io.github.resilience4j.bulkhead.configure;

import org.junit.*;
import org.mockito.*;
import reactor.core.publisher.*;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.timelimiter.*;

import org.junit.runner.RunWith;
import org.aspectj.lang.ProceedingJoinPoint;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.util.concurrent.*;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    public void testThrowableIsThrownAndCaught() throws Throwable {
        String expectedMessage = "Test Exception";
        Throwable throwable = new Throwable(expectedMessage);

        when(proceedingJoinPoint.proceed()).thenThrow(throwable);

        try {
            plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);
        } catch (Exception ex) {
            assertThat(ex.getCause()).isEqualTo(throwable);
            assertThat(ex.getCause().getMessage()).isEqualTo(expectedMessage);
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
        } catch (Exception ex) {
            assertThat(ex.getCause()).isEqualTo(bulkheadFullException);
            assertThat(ex.getCause().getMessage()).isEqualTo(expectedMessage);
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
