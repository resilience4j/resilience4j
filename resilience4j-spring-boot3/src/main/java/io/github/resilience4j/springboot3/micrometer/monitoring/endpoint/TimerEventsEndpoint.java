/*
 * Copyright 2023 Mariusz Kopylec
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
package io.github.resilience4j.springboot3.micrometer.monitoring.endpoint;

import io.github.resilience4j.common.micrometer.monitoring.endpoint.TimerEventDTOFactory;
import io.github.resilience4j.common.micrometer.monitoring.endpoint.TimerEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.micrometer.event.TimerEvent;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API endpoint to retrieve timer events
 */
@Endpoint(id = "timerevents")
public class TimerEventsEndpoint {

    private final EventConsumerRegistry<TimerEvent> eventConsumerRegistry;

    public TimerEventsEndpoint(EventConsumerRegistry<TimerEvent> eventConsumerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
    }

    /**
     * @return all timer generated events
     */
    @ReadOperation
    public TimerEventsEndpointResponse getAllTimerEvents() {
        return new TimerEventsEndpointResponse(eventConsumerRegistry.getAllEventConsumer().stream()
                .flatMap(CircularEventConsumer::getBufferedEventsStream)
                .sorted(Comparator.comparing(TimerEvent::getCreationTime))
                .map(TimerEventDTOFactory::createTimerEventDTO)
                .collect(Collectors.toList()));
    }

    /**
     * @param name backend name
     * @return the timer events generated for this backend
     */
    @ReadOperation
    public TimerEventsEndpointResponse getEventsFilteredByTimerName(@Selector String name) {
        return new TimerEventsEndpointResponse(getTimerEvents(name).stream()
                .map(TimerEventDTOFactory::createTimerEventDTO)
                .collect(Collectors.toList()));
    }

    /**
     * @param name      backend name
     * @param eventType timer event type
     * @return the matching generated timer events
     */
    @ReadOperation
    public TimerEventsEndpointResponse getEventsFilteredByTimerNameAndEventType(
            @Selector String name, @Selector String eventType) {
        return new TimerEventsEndpointResponse(getTimerEvents(name).stream()
                .filter(event -> event.getEventType() == TimerEvent.Type.valueOf(eventType.toUpperCase()))
                .map(TimerEventDTOFactory::createTimerEventDTO)
                .collect(Collectors.toList()));
    }

    private List<TimerEvent> getTimerEvents(String name) {
        final CircularEventConsumer<TimerEvent> syncEvents = eventConsumerRegistry
                .getEventConsumer(name);
        if (syncEvents != null) {
            return syncEvents.getBufferedEventsStream()
                    .filter(event -> event.getTimerName().equals(name))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
