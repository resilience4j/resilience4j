package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PlainObjectBulkheadAspectExtTest {

    private final String BULKHEAD_NAME = "test";
    private final String TEST_METHOD = "testMethod";

    @Mock
    ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    PlainObjectBulkheadAspectExt plainObjectBulkHeadAspectExt;

    @Test
    public void testBulkheadReturnsPlainObject() throws Throwable{
        String expected = "Plain Result";

        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Bulkhead bulkhead = Bulkhead.ofDefaults(BULKHEAD_NAME);
        Object actual = plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testBulkheadLimits() throws Throwable {
        String expected = "Bulkhead Limit Exceeded";
        Bulkhead bulkhead = Bulkhead.ofDefaults(BULKHEAD_NAME);

        when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException(expected));

        try {
            plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage()).isEqualTo(expected);
        }
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

    @Test
    public void testBulkheadConcurrency() throws Throwable {
        String expected = "Plain Result";
        int numberOfThreads = 5;
        int maxConcurrentCalls = 3;
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        Bulkhead bulkhead = Bulkhead.of(BULKHEAD_NAME,
                BulkheadConfig.custom()
                        .maxConcurrentCalls(maxConcurrentCalls)
                        .build());

        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    Object actual = plainObjectBulkHeadAspectExt.handle(proceedingJoinPoint, bulkhead, TEST_METHOD);
                    assertThat(actual).isEqualTo(expected);
                } catch (Throwable ex) {
                    throw new RuntimeException(ex);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();
        executorService.shutdown();
    }
}
