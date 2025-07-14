package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

/**
 * Tests that verify CircuitBreakerStateMachine correctly handles both platform and virtual threads
 * based on the system property {@code resilience4j.thread.type} configuration.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class CircuitBreakerStateMachineThreadModeTest extends ThreadModeTestBase {

    private static final Duration WAIT_DURATION = Duration.ofMillis(100);

    public CircuitBreakerStateMachineThreadModeTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }
    
    @Override
    public void setUpThreadMode() {
        super.setUpThreadMode();
        SchedulerFactory.getInstance().reset();
    }
    
    @Override
    public void cleanUpThreadMode() {
        super.cleanUpThreadMode();
        SchedulerFactory.getInstance().reset();
    }
    
    @Test
    public void shouldUseCorrectThreadTypeForAutomaticTransitionFromOpenToHalfOpen() throws Exception {
        // Thread mode configured automatically by ThreadModeTestBase
        
        // Set up latch to wait for transition
        CountDownLatch transitionLatch = new CountDownLatch(1);
        AtomicBoolean threadTypeMatches = new AtomicBoolean(false);
        
        // Create CircuitBreaker with automatic transition and a state change listener
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("test" + threadType,
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(WAIT_DURATION)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());
                
        // Add a state transition listener
        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
                // Record if we're running on the expected thread type
                boolean expectedThreadType = isVirtualThreadMode() ? 
                    Thread.currentThread().isVirtual() : 
                    !Thread.currentThread().isVirtual();
                threadTypeMatches.set(expectedThreadType);
                transitionLatch.countDown();
            }
        });
        
        // Transition to OPEN state which will schedule automatic transition to HALF_OPEN
        circuitBreaker.transitionToOpenState();
        
        // Wait for automatic transition to HALF_OPEN
        assertTrue("Transition to HALF_OPEN did not occur within expected time", 
            transitionLatch.await(1, TimeUnit.SECONDS));
        
        // Verify that the transition happened on the expected thread type
        assertTrue("Automatic transition should have executed on " + threadType + " thread",
            threadTypeMatches.get());
    }
    
    
    @Test
    public void shouldMaintainConsistentThreadTypeAcrossMultipleCircuitBreakers() throws Exception {
        // Thread mode configured automatically by ThreadModeTestBase
        
        // Set up latches for transitions
        CountDownLatch firstTransitionLatch = new CountDownLatch(1);
        CountDownLatch secondTransitionLatch = new CountDownLatch(1);
        
        AtomicBoolean firstThreadTypeMatches = new AtomicBoolean(false);
        AtomicBoolean secondThreadTypeMatches = new AtomicBoolean(false);
        
        // Create first CircuitBreaker with automatic transition
        CircuitBreaker firstCircuitBreaker = new CircuitBreakerStateMachine("first" + threadType,
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(WAIT_DURATION)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());
                
        // Add a state transition listener to the first circuit breaker
        firstCircuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
                boolean expectedThreadType = isVirtualThreadMode() ? 
                    Thread.currentThread().isVirtual() : 
                    !Thread.currentThread().isVirtual();
                firstThreadTypeMatches.set(expectedThreadType);
                firstTransitionLatch.countDown();
            }
        });
        
        // Create second CircuitBreaker with automatic transition
        CircuitBreaker secondCircuitBreaker = new CircuitBreakerStateMachine("second" + threadType,
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(WAIT_DURATION)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());
                
        // Add a state transition listener to the second circuit breaker
        secondCircuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
                boolean expectedThreadType = isVirtualThreadMode() ? 
                    Thread.currentThread().isVirtual() : 
                    !Thread.currentThread().isVirtual();
                secondThreadTypeMatches.set(expectedThreadType);
                secondTransitionLatch.countDown();
            }
        });
        
        // Transition both CBs to OPEN state which will schedule automatic transition to HALF_OPEN
        firstCircuitBreaker.transitionToOpenState();
        secondCircuitBreaker.transitionToOpenState();
        
        // Wait for both transitions
        assertTrue("First transition to HALF_OPEN did not occur within expected time", 
            firstTransitionLatch.await(1, TimeUnit.SECONDS));
        assertTrue("Second transition to HALF_OPEN did not occur within expected time", 
            secondTransitionLatch.await(1, TimeUnit.SECONDS));
            
        // Verify both transitions used the same thread type
        assertTrue("First transition should have executed on " + threadType + " thread",
            firstThreadTypeMatches.get());
        assertTrue("Second transition should have executed on " + threadType + " thread",
            secondThreadTypeMatches.get());
    }
}