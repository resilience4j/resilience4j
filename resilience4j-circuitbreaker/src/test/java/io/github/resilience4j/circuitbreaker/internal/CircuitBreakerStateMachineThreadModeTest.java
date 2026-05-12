package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify CircuitBreakerStateMachine correctly handles both platform and virtual threads
 * based on the system property {@code resilience4j.thread.type} configuration.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
@ExtendWith(ThreadModeExtension.class)
class CircuitBreakerStateMachineThreadModeTest {

    private static final Duration WAIT_DURATION = Duration.ofMillis(100);

    @BeforeEach
    void setUp() {
        SchedulerFactory.getInstance().reset();
    }

    @AfterEach
    void tearDown() {
        SchedulerFactory.getInstance().reset();
    }

    @TestTemplate
    void shouldUseCorrectThreadTypeForAutomaticTransitionFromOpenToHalfOpen(ThreadType threadType) throws Exception {
        CountDownLatch transitionLatch = new CountDownLatch(1);
        AtomicBoolean threadTypeMatches = new AtomicBoolean(false);

        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("test-" + threadType,
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(WAIT_DURATION)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());

        circuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
                boolean expectedThreadType = threadType == ThreadType.VIRTUAL
                    ? Thread.currentThread().isVirtual()
                    : !Thread.currentThread().isVirtual();
                threadTypeMatches.set(expectedThreadType);
                transitionLatch.countDown();
            }
        });

        circuitBreaker.transitionToOpenState();

        assertThat(transitionLatch.await(1, TimeUnit.SECONDS))
            .as("Transition to HALF_OPEN did not occur within expected time")
            .isTrue();
        assertThat(threadTypeMatches.get())
            .as("Automatic transition should have executed on %s thread", threadType)
            .isTrue();
    }

    @TestTemplate
    void shouldMaintainConsistentThreadTypeAcrossMultipleCircuitBreakers(ThreadType threadType) throws Exception {
        CountDownLatch firstTransitionLatch = new CountDownLatch(1);
        CountDownLatch secondTransitionLatch = new CountDownLatch(1);
        AtomicBoolean firstThreadTypeMatches = new AtomicBoolean(false);
        AtomicBoolean secondThreadTypeMatches = new AtomicBoolean(false);

        CircuitBreaker firstCircuitBreaker = new CircuitBreakerStateMachine("first-" + threadType,
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(WAIT_DURATION)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());

        firstCircuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
                boolean expectedThreadType = threadType == ThreadType.VIRTUAL
                    ? Thread.currentThread().isVirtual()
                    : !Thread.currentThread().isVirtual();
                firstThreadTypeMatches.set(expectedThreadType);
                firstTransitionLatch.countDown();
            }
        });

        CircuitBreaker secondCircuitBreaker = new CircuitBreakerStateMachine("second-" + threadType,
            CircuitBreakerConfig.custom()
                .waitDurationInOpenState(WAIT_DURATION)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build());

        secondCircuitBreaker.getEventPublisher().onStateTransition(event -> {
            if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
                boolean expectedThreadType = threadType == ThreadType.VIRTUAL
                    ? Thread.currentThread().isVirtual()
                    : !Thread.currentThread().isVirtual();
                secondThreadTypeMatches.set(expectedThreadType);
                secondTransitionLatch.countDown();
            }
        });

        firstCircuitBreaker.transitionToOpenState();
        secondCircuitBreaker.transitionToOpenState();

        assertThat(firstTransitionLatch.await(1, TimeUnit.SECONDS))
            .as("First transition to HALF_OPEN did not occur within expected time")
            .isTrue();
        assertThat(secondTransitionLatch.await(1, TimeUnit.SECONDS))
            .as("Second transition to HALF_OPEN did not occur within expected time")
            .isTrue();
        assertThat(firstThreadTypeMatches.get())
            .as("First transition should have executed on %s thread", threadType)
            .isTrue();
        assertThat(secondThreadTypeMatches.get())
            .as("Second transition should have executed on %s thread", threadType)
            .isTrue();
    }
}
