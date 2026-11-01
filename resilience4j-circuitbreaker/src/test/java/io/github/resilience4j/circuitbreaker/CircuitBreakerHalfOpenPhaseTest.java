/*
 *
 *  Copyright 2026 authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.HalfOpenPhase;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnHalfOpenPhaseTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnHalfOpenPhaseTransitionEvent.Reason;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.core.EventConsumer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class CircuitBreakerHalfOpenPhaseTest {

    @Test
    void existingImplementationsDefaultToInactivePhase() {
        CircuitBreaker customImplementation = mock(CircuitBreaker.class, CALLS_REAL_METHODS);

        assertThat(customImplementation.getHalfOpenPhase()).isEqualTo(HalfOpenPhase.inactive());
        assertThat(customImplementation.getHalfOpenPhase().isActive()).isFalse();
        assertThat(customImplementation.getHalfOpenPhase().getPhase()).isZero();
        assertThat(customImplementation.getHalfOpenPhase().getNumberOfPhases()).isZero();
        assertThat(customImplementation.getHalfOpenPhase().getPermittedTrafficPercentage()).isZero();
    }

    @Test
    void snapshotKeepsPhaseAndTrafficTargetAsAValue() {
        HalfOpenPhase phase = new HalfOpenPhase(2, 3, 4);

        assertThat(phase.isActive()).isTrue();
        assertThat(phase.getPhase()).isEqualTo(2);
        assertThat(phase.getNumberOfPhases()).isEqualTo(3);
        assertThat(phase.getPermittedTrafficPercentage()).isEqualTo(4);
        assertThat(phase).isEqualTo(new HalfOpenPhase(2, 3, 4));
        assertThat(phase.hashCode()).isEqualTo(new HalfOpenPhase(2, 3, 4).hashCode());
        assertThat(phase).isNotEqualTo(new HalfOpenPhase(1, 3, 2));
        assertThat(new HalfOpenPhase(0, 0, 0)).isEqualTo(HalfOpenPhase.inactive());
    }

    @Test
    void snapshotRejectsMixedInactiveAndInvalidPhaseValues() {
        int[][] invalid = {{0, 3, 2}, {1, 0, 2}, {2, 1, 2}, {1, 3, 0}, {-1, 3, 2}, {1, 3, 101}};

        for (int[] values : invalid) {
            assertThatThrownBy(() -> new HalfOpenPhase(values[0], values[1], values[2]))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void existingStateValuesKeepTheirMetricOrders() {
        Map<String, Integer> orders = Arrays.stream(CircuitBreaker.State.values())
            .collect(Collectors.toMap(Enum::name, CircuitBreaker.State::getOrder));

        assertThat(orders).isEqualTo(Map.of("CLOSED", 0, "OPEN", 1, "HALF_OPEN", 2,
            "DISABLED", 3, "FORCED_OPEN", 4, "METRICS_ONLY", 5));
    }

    @Test
    void typedSubscriptionWorksWithExistingCustomPublisher() {
        CircuitBreaker.EventPublisher customPublisher =
            mock(CircuitBreaker.EventPublisher.class, CALLS_REAL_METHODS);
        AtomicReference<EventConsumer<CircuitBreakerEvent>> subscription = new AtomicReference<>();
        doAnswer(invocation -> {
            subscription.set(invocation.getArgument(0));
            return null;
        }).when(customPublisher).onEvent(any());
        List<CircuitBreakerOnHalfOpenPhaseTransitionEvent> events = new ArrayList<>();

        assertThat(customPublisher.onHalfOpenPhaseTransition(events::add)).isSameAs(customPublisher);
        subscription.get().consumeEvent(new CircuitBreakerOnSuccessEvent("test", Duration.ZERO));
        HalfOpenPhase previous = new HalfOpenPhase(1, 3, 2);
        HalfOpenPhase current = new HalfOpenPhase(2, 3, 4);
        CircuitBreakerOnHalfOpenPhaseTransitionEvent transition =
            new CircuitBreakerOnHalfOpenPhaseTransitionEvent("test", previous, current, Reason.ADVANCED);
        subscription.get().consumeEvent(transition);

        assertThat(events).containsExactly(transition);
        assertThat(transition.getPreviousPhase()).isEqualTo(previous);
        assertThat(transition.getPhase()).isEqualTo(current);
        assertThat(transition.getReason()).isEqualTo(Reason.ADVANCED);
        assertThat(transition.getEventType())
            .isEqualTo(CircuitBreakerEvent.Type.HALF_OPEN_PHASE_TRANSITION);
        assertThat(transition.getCircuitBreakerName()).isEqualTo("test");
        assertThat(transition.getCreationTime()).isNotNull();
        assertThatThrownBy(() -> customPublisher.onHalfOpenPhaseTransition(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nativeTypedSubscriptionDeduplicatesTheSameConsumer() {
        CircuitBreaker breaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().build()).build());
        List<CircuitBreakerOnHalfOpenPhaseTransitionEvent> events = new ArrayList<>();
        EventConsumer<CircuitBreakerOnHalfOpenPhaseTransitionEvent> consumer = events::add;

        assertThat(breaker.getEventPublisher().onHalfOpenPhaseTransition(consumer)
            .onHalfOpenPhaseTransition(consumer)).isSameAs(breaker.getEventPublisher());
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getReason()).isEqualTo(Reason.ENTERED);
        assertThatThrownBy(() -> breaker.getEventPublisher().onHalfOpenPhaseTransition(null))
            .isInstanceOf(NullPointerException.class);
    }
}
