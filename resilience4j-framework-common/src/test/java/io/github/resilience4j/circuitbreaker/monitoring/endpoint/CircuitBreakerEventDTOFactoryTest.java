package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.event.*;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTOFactory;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.StateTransition;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerEventDTOFactoryTest {

    @Test
    public void shouldMapCircuitBreakerOnSuccessEvent() {
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
    public void shouldMapCircuitBreakerOnErrorEvent() {
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
    public void shouldMapCircuitBreakerOnCallNotPermittedEvent() {
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
    public void shouldMapCircuitBreakerOnStateTransitionEvent() {
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
    public void shouldMapCircuitBreakerOnIgnoredErrorEvent() {
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
    public void shouldMapCircuitBreakerOnResetEvent() {
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
