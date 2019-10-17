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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;


@Controller
@RequestMapping(value = "circuitbreaker/")
public class CircuitBreakerEventsEndpoint {

    private final EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry;

    public CircuitBreakerEventsEndpoint(
        EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
    }

    @GetMapping(value = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CircuitBreakerEventsEndpointResponse getAllCircuitBreakerEvents() {
        return new CircuitBreakerEventsEndpointResponse(eventConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(CircuitBreakerEvent::getCreationTime))
            .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO).toJavaList());
    }

    @GetMapping(value = "events/{circuitBreakerName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CircuitBreakerEventsEndpointResponse getEventsFilteredByCircuitBreakerName(
        @PathVariable("circuitBreakerName") String circuitBreakerName) {
        return new CircuitBreakerEventsEndpointResponse(getCircuitBreakerEvents(circuitBreakerName)
            .filter(event -> event.getCircuitBreakerName().equals(circuitBreakerName))
            .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO).toJavaList());
    }

    @GetMapping(value = "events/{circuitBreakerName}/{eventType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public CircuitBreakerEventsEndpointResponse getEventsFilteredByCircuitBreakerNameAndEventType(
        @PathVariable("circuitBreakerName") String circuitBreakerName,
        @PathVariable("eventType") String eventType) {
        return new CircuitBreakerEventsEndpointResponse(getCircuitBreakerEvents(circuitBreakerName)
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
