/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventDTO;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Endpoint(id = "ratelimiterevents")
public class RateLimiterEventsEndpoint {

    private final EventConsumerRegistry<RateLimiterEvent> eventsConsumerRegistry;

    public RateLimiterEventsEndpoint(
        EventConsumerRegistry<RateLimiterEvent> eventsConsumerRegistry) {
        this.eventsConsumerRegistry = eventsConsumerRegistry;
    }

    @ReadOperation
    public RateLimiterEventsEndpointResponse getAllRateLimiterEvents() {
        return new RateLimiterEventsEndpointResponse(eventsConsumerRegistry.getAllEventConsumer().stream()
            .flatMap(CircularEventConsumer::getBufferedEventsStream)
            .sorted(Comparator.comparing(RateLimiterEvent::getCreationTime))
            .map(RateLimiterEventDTO::createRateLimiterEventDTO)
            .collect(Collectors.toList()));
    }

    @ReadOperation
    public RateLimiterEventsEndpointResponse getEventsFilteredByRateLimiterName(
        @Selector String name) {
        return new RateLimiterEventsEndpointResponse(getRateLimiterEvents(name).stream()
            .map(RateLimiterEventDTO::createRateLimiterEventDTO)
            .collect(Collectors.toList()));
    }

    @ReadOperation
    public RateLimiterEventsEndpointResponse getEventsFilteredByRateLimiterNameAndEventType(
        @Selector String name,
        @Selector String eventType) {
        RateLimiterEvent.Type targetType = RateLimiterEvent.Type.valueOf(eventType.toUpperCase());
        return new RateLimiterEventsEndpointResponse(getRateLimiterEvents(name).stream()
            .filter(event -> event.getEventType() == targetType)
            .map(RateLimiterEventDTO::createRateLimiterEventDTO)
            .collect(Collectors.toList()));
    }

    private List<RateLimiterEvent> getRateLimiterEvents(String name) {
        CircularEventConsumer<RateLimiterEvent> eventConsumer = eventsConsumerRegistry
            .getEventConsumer(name);
        if (eventConsumer != null) {
            return eventConsumer.getBufferedEventsStream()
                .filter(event -> event.getRateLimiterName().equals(name))
                .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }
}
