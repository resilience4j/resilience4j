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
package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import java.time.Duration;

import io.github.resilience4j.circuitbreaker.CircuitBreaker.HalfOpenPhase;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnHalfOpenPhaseTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTOFactory;
import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnHalfOpenPhaseTransitionEvent.Reason.ADVANCED;
import static org.assertj.core.api.Assertions.assertThat;

class HalfOpenPhaseEventDTOTest {

    @Test
    void actuatorEventConversionPreservesPhaseTransitionDetails() {
        HalfOpenPhase before = new HalfOpenPhase(1, 3, 4);
        HalfOpenPhase after = new HalfOpenPhase(2, 3, 8);
        var event = new CircuitBreakerOnHalfOpenPhaseTransitionEvent("backend", before, after, ADVANCED);

        var dto = CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(event);

        assertThat(dto.getCircuitBreakerName()).isEqualTo("backend");
        assertThat(dto.getType()).isEqualTo(CircuitBreakerEvent.Type.HALF_OPEN_PHASE_TRANSITION);
        assertThat(dto.getCreationTime()).isEqualTo(event.getCreationTime().toString());
        assertThat(dto.getPreviousHalfOpenPhase()).isSameAs(before);
        assertThat(dto.getHalfOpenPhase()).isSameAs(after);
        assertThat(dto.getHalfOpenPhaseTransitionReason()).isEqualTo(ADVANCED);
        assertThat(dto.getStateTransition()).isNull();
    }

    @Test
    void existingEventsHaveNoPhaseTransitionDetails() {
        var dto = CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(
            new CircuitBreakerOnSuccessEvent("backend", Duration.ofMillis(5)));

        assertThat(dto.getType()).isEqualTo(CircuitBreakerEvent.Type.SUCCESS);
        assertThat(dto.getDurationInMs()).isEqualTo(5);
        assertThat(dto.getPreviousHalfOpenPhase()).isNull();
        assertThat(dto.getHalfOpenPhase()).isNull();
        assertThat(dto.getHalfOpenPhaseTransitionReason()).isNull();
    }
}
