package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Thread mode compatibility test to verify CircuitBreaker works consistently
 * with both platform and virtual threads.
 * 
 * This parameterized test validates that CircuitBreaker functionality is
 * identical across both thread modes, ensuring safe operation in either environment.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class CircuitBreakerThreadModeCompatibilityTest extends ThreadModeTestBase {

    public CircuitBreakerThreadModeCompatibilityTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }
    
    @Test
    public void shouldWorkWithBothThreadModes() throws ExecutionException, InterruptedException {
        // Given - Thread mode configured automatically by ThreadModeTestBase
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .slidingWindowSize(2)
            .build();
        
        CircuitBreaker circuitBreaker = CircuitBreaker.of("test-" + threadType, config);
        
        // When - Test successful operation
        String result = circuitBreaker.executeSupplier(() -> "Hello from " + Thread.currentThread().getName());
        
        // Then
        assertThat(result).contains("Hello from");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        
        System.out.println(threadType + " thread test: State=" + circuitBreaker.getState() +
                          ", SuccessfulCalls=" + circuitBreaker.getMetrics().getNumberOfSuccessfulCalls());
    }
    
    @Test
    public void shouldHandleFailuresConsistentlyInBothModes() {
        // Thread mode configured automatically by ThreadModeTestBase
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .slidingWindowSize(2)
            .build();
        
        CircuitBreaker circuitBreaker = CircuitBreaker.of(threadType + "-failure", config);
        int failures = testFailureBehavior(circuitBreaker, threadType);
        
        // Should handle failures consistently regardless of thread mode
        assertThat(failures).isEqualTo(3); // Expected 3 failures
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN); // Should be OPEN after failures
    }
    
    @Test
    public void shouldHandleCompletableFutureOperationsInBothModes() throws ExecutionException, InterruptedException {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(100))
            .slidingWindowSize(2)
            .build();
        
        // Thread mode configured automatically by ThreadModeTestBase
        CircuitBreaker circuitBreaker = CircuitBreaker.of(threadType + "-async", config);
        CompletionStage<String> future = circuitBreaker.executeCompletionStage(() -> 
            CompletableFuture.supplyAsync(() -> threadType + " async result"));
        String result = future.toCompletableFuture().get();
        
        // Should complete successfully in both thread modes
        assertThat(result).isEqualTo(threadType + " async result");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        System.out.println(threadType + " async: " + result);
    }
    
    @Test
    public void shouldHandleStateTransitionsConsistentlyInBothModes() throws InterruptedException {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofMillis(50))
            .slidingWindowSize(2)
            .minimumNumberOfCalls(2)
            .build();
        
        // Thread mode configured automatically by ThreadModeTestBase
        CircuitBreaker circuitBreaker = CircuitBreaker.of(threadType + "-state", config);
        CircuitBreaker.State finalState = testStateTransitions(circuitBreaker, threadType);
        
        // Should handle state transitions consistently in both thread modes
        // The final state depends on the last operation but should be consistent
        assertThat(finalState).isIn(CircuitBreaker.State.CLOSED, CircuitBreaker.State.HALF_OPEN);
    }
    
    private int testFailureBehavior(CircuitBreaker circuitBreaker, ThreadType mode) {
        AtomicInteger failures = new AtomicInteger(0);
        
        // Cause some failures
        for (int i = 0; i < 3; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception e) {
                failures.incrementAndGet();
            }
        }
        
        System.out.println(mode + " mode failures: " + failures.get() + ", State: " + circuitBreaker.getState());
        return failures.get();
    }
    
    private CircuitBreaker.State testStateTransitions(CircuitBreaker circuitBreaker, ThreadType mode) throws InterruptedException {
        // Start in CLOSED state
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        
        // Cause failures to open the circuit
        for (int i = 0; i < 2; i++) {
            try {
                circuitBreaker.executeSupplier(() -> {
                    throw new RuntimeException("Test failure");
                });
            } catch (Exception ignored) {}
        }
        
        // Should be OPEN now
        CircuitBreaker.State openState = circuitBreaker.getState();
        
        // Wait for transition to HALF_OPEN
        Thread.sleep(60);
        
        // Trigger transition to HALF_OPEN by attempting an operation
        try {
            circuitBreaker.executeSupplier(() -> "success");
        } catch (Exception ignored) {}
        
        CircuitBreaker.State finalState = circuitBreaker.getState();
        System.out.println(mode + " state transitions: CLOSED -> " + openState + " -> " + finalState);
        
        return finalState;
    }
}