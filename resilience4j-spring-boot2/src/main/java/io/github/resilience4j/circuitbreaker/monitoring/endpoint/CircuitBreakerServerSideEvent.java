/*
 * Copyright 2020 Vijay Ram
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTOFactory;
import io.vavr.collection.Seq;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.function.Function;

import static io.github.resilience4j.reactor.adapter.ReactorAdapter.toFlux;

/**
 * This class is used to produce Circuit breaker events as streams.
 * <p>
 * The following endpoints are automatically generated and events are produced as Server Sent Event(SSE)
 * curl -vv http://localhost:8090/actuator/stream-circuitbreaker-events
 * curl -vv http://localhost:8090/actuator/stream-circuitbreaker-events/{circuitbreakername}
 * curl -vv http://localhost:8090/actuator/stream-circuitbreaker-events/{circuitbreakername}/{errorType}
 */

@Endpoint(id = "stream-circuitbreaker-events")
public class CircuitBreakerServerSideEvent {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    private final ObjectMapper jsonMapper = new ObjectMapper();

    public CircuitBreakerServerSideEvent(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @ReadOperation(produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> getAllCircuitBreakerServerSideEvent() {
        Seq<Flux<CircuitBreakerEvent>> eventStreams = circuitBreakerRegistry.getAllCircuitBreakers()
            .map(
                circuitBreaker -> toFlux(circuitBreaker.getEventPublisher())
            );
        Function<CircuitBreakerEvent, String> data = getCircuitBreakerEventStringFunction();
        return Flux.merge(eventStreams).map(
            cbEvent -> ServerSentEvent.<String>builder()
                .id(cbEvent.getCircuitBreakerName())
                .event(cbEvent.getEventType().name())
                .data(data.apply(cbEvent))
                .build()
        );
    }

    @ReadOperation(produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> getEventsFilteredByCircuitBreakerName(
        @Selector String name) {

        CircuitBreaker circuitBreaker = getCircuitBreaker(name);
        Flux<CircuitBreakerEvent> eventStream = toFlux(circuitBreaker.getEventPublisher());
        Function<CircuitBreakerEvent, String> data = getCircuitBreakerEventStringFunction();
        return eventStream.map(
            cbEvent -> ServerSentEvent.<String>builder()
                .id(cbEvent.getCircuitBreakerName())
                .event(cbEvent.getEventType().name())
                .data(data.apply(cbEvent))
                .build()
        );
    }

    @ReadOperation(produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> getEventsFilteredByCircuitBreakerNameAndEventType(
        @Selector String name, @Selector String eventType) {

        CircuitBreaker circuitBreaker = getCircuitBreaker(name);
        Flux<CircuitBreakerEvent> eventStream = toFlux(circuitBreaker.getEventPublisher())
            .filter(
                event -> event.getEventType() == CircuitBreakerEvent.Type.valueOf(eventType.toUpperCase())
            );
        Function<CircuitBreakerEvent, String> data = getCircuitBreakerEventStringFunction();
        return eventStream.map(cbEvent -> ServerSentEvent.<String>builder()
            .id(cbEvent.getCircuitBreakerName())
            .event(cbEvent.getEventType().name())
            .data(data.apply(cbEvent))
            .build()
        );
    }

    private Function<CircuitBreakerEvent, String> getCircuitBreakerEventStringFunction() {
        return cbEvent -> {
            try {
                return jsonMapper.writeValueAsString(
                    CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(cbEvent)
                );
            } catch (JsonProcessingException e) {
                /* ignore silently */
            }
            return "";
        };
    }

    private CircuitBreaker getCircuitBreaker(String circuitBreakerName) {
        return circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
    }
}
