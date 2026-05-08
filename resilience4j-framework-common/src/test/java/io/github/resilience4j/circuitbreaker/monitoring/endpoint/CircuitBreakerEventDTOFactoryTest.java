/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnCallNotPermittedEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnIgnoredErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnResetEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTOFactory;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.ERROR;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.IGNORED_ERROR;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.RESET;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.STATE_TRANSITION;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerEventDTOFactoryTest {

    @Test
    void shouldMapCircuitBreakerOnSuccessEvent() {
        CircuitBreakerOnSuccessEvent event = new CircuitBreakerOnSuccessEvent("name",
            Duration.ofSeconds(5));

        CircuitBreakerEventDTO circuitBreakerEventDTO = CircuitBreakerEventDTOFactory
            .createCircuitBreakerEventDTO(event);

        assertThat(circuitBreakerEventDTO.getCircuitBreakerName()).isEqualTo("name");
        assertThat(circuitBreakerEventDTO.getDurationInMs()).isEqualTo(5000);
        assertThat(circuitBreakerEventDTO.getType()).isEqualTo(SUCCESS);
        assertThat(circuitBreakerEventDTO.getErrorMessage()).isNull();
        assertThat(circuitBreakerEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapCircuitBreakerOnErrorEvent() {
        CircuitBreakerOnErrorEvent event = new CircuitBreakerOnErrorEvent("name",
            Duration.ofSeconds(5), new IOException("Error Message"));

        CircuitBreakerEventDTO circuitBreakerEventDTO = CircuitBreakerEventDTOFactory
            .createCircuitBreakerEventDTO(event);

        assertThat(circuitBreakerEventDTO.getCircuitBreakerName()).isEqualTo("name");
        assertThat(circuitBreakerEventDTO.getDurationInMs()).isEqualTo(5000);
        assertThat(circuitBreakerEventDTO.getType()).isEqualTo(ERROR);
        assertThat(circuitBreakerEventDTO.getErrorMessage())
            .isEqualTo("java.io.IOException: Error Message");
        assertThat(circuitBreakerEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapCircuitBreakerOnCallNotPermittedEvent() {
        CircuitBreakerOnCallNotPermittedEvent event = new CircuitBreakerOnCallNotPermittedEvent(
            "name");

        CircuitBreakerEventDTO circuitBreakerEventDTO = CircuitBreakerEventDTOFactory
            .createCircuitBreakerEventDTO(event);

        assertThat(circuitBreakerEventDTO.getCircuitBreakerName()).isEqualTo("name");
        assertThat(circuitBreakerEventDTO.getDurationInMs()).isNull();
        assertThat(circuitBreakerEventDTO.getType()).isEqualTo(NOT_PERMITTED);
        assertThat(circuitBreakerEventDTO.getErrorMessage()).isNull();
        assertThat(circuitBreakerEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapCircuitBreakerOnStateTransitionEvent() {
        CircuitBreakerOnStateTransitionEvent event = new CircuitBreakerOnStateTransitionEvent(
            "name", StateTransition.CLOSED_TO_OPEN);

        CircuitBreakerEventDTO circuitBreakerEventDTO = CircuitBreakerEventDTOFactory
            .createCircuitBreakerEventDTO(event);

        assertThat(circuitBreakerEventDTO.getCircuitBreakerName()).isEqualTo("name");
        assertThat(circuitBreakerEventDTO.getDurationInMs()).isNull();
        assertThat(circuitBreakerEventDTO.getType()).isEqualTo(STATE_TRANSITION);
        assertThat(circuitBreakerEventDTO.getErrorMessage()).isNull();
        assertThat(circuitBreakerEventDTO.getStateTransition())
            .isEqualTo(StateTransition.CLOSED_TO_OPEN);
        assertThat(circuitBreakerEventDTO.getCreationTime()).isNotNull();
    }


    @Test
    void shouldMapCircuitBreakerOnIgnoredErrorEvent() {
        CircuitBreakerOnIgnoredErrorEvent event = new CircuitBreakerOnIgnoredErrorEvent("name",
            Duration.ofSeconds(5), new IOException("Error Message"));

        CircuitBreakerEventDTO circuitBreakerEventDTO = CircuitBreakerEventDTOFactory
            .createCircuitBreakerEventDTO(event);

        assertThat(circuitBreakerEventDTO.getCircuitBreakerName()).isEqualTo("name");
        assertThat(circuitBreakerEventDTO.getDurationInMs()).isEqualTo(5000);
        assertThat(circuitBreakerEventDTO.getType()).isEqualTo(IGNORED_ERROR);
        assertThat(circuitBreakerEventDTO.getErrorMessage())
            .isEqualTo("java.io.IOException: Error Message");
        assertThat(circuitBreakerEventDTO.getCreationTime()).isNotNull();
    }

    @Test
    void shouldMapCircuitBreakerOnResetEvent() {
        CircuitBreakerOnResetEvent event = new CircuitBreakerOnResetEvent("name");

        CircuitBreakerEventDTO circuitBreakerEventDTO = CircuitBreakerEventDTOFactory
            .createCircuitBreakerEventDTO(event);

        assertThat(circuitBreakerEventDTO.getCircuitBreakerName()).isEqualTo("name");
        assertThat(circuitBreakerEventDTO.getDurationInMs()).isNull();
        assertThat(circuitBreakerEventDTO.getType()).isEqualTo(RESET);
        assertThat(circuitBreakerEventDTO.getErrorMessage()).isNull();
        assertThat(circuitBreakerEventDTO.getCreationTime()).isNotNull();
    }
}
