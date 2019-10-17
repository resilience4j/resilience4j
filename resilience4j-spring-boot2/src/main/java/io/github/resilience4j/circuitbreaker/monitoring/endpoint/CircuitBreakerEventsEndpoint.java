/*
 * Copyright 2017 Robert Winkler
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


import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTOFactory;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.vavr.collection.List;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.Comparator;


@Endpoint(id = "circuitbreakerevents")
public class CircuitBreakerEventsEndpoint {

    private final EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry;

    public CircuitBreakerEventsEndpoint(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
    }

    @ReadOperation
    public CircuitBreakerEventsEndpointResponse getAllCircuitBreakerEvents() {
        return new CircuitBreakerEventsEndpointResponse(eventConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(CircuitBreakerEvent::getCreationTime))
            .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO).toJavaList());
    }

    @ReadOperation
    public CircuitBreakerEventsEndpointResponse getEventsFilteredByCircuitBreakerName(
        @Selector String name) {
        return new CircuitBreakerEventsEndpointResponse(getCircuitBreakerEvents(name)
            .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO).toJavaList());
    }

    @ReadOperation
    public CircuitBreakerEventsEndpointResponse getEventsFilteredByCircuitBreakerNameAndEventType(
        @Selector String name, @Selector String eventType) {
        return new CircuitBreakerEventsEndpointResponse(getCircuitBreakerEvents(name)
            .filter(event -> event.getEventType() == CircuitBreakerEvent.Type
                .valueOf(eventType.toUpperCase()))
            .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO).toJavaList());
    }

    private List<CircuitBreakerEvent> getCircuitBreakerEvents(String circuitBreakerName) {
        CircularEventConsumer<CircuitBreakerEvent> eventConsumer = eventConsumerRegistry
            .getEventConsumer(circuitBreakerName);
        if (eventConsumer != null) {
            return eventConsumer.getBufferedEvents()
                .filter(event -> event.getCircuitBreakerName().equals(circuitBreakerName));
        } else {
            return List.empty();
        }
    }
}
