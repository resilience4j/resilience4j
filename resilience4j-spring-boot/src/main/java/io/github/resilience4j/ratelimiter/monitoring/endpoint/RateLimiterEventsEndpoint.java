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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping(value = "ratelimiter/")
public class RateLimiterEventsEndpoint {

    private final EventConsumerRegistry<RateLimiterEvent> eventsConsumerRegistry;

    public RateLimiterEventsEndpoint(
        EventConsumerRegistry<RateLimiterEvent> eventsConsumerRegistry) {
        this.eventsConsumerRegistry = eventsConsumerRegistry;
    }


    @GetMapping(value = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RateLimiterEventsEndpointResponse getAllRateLimiterEvents() {
        List<RateLimiterEventDTO> eventsList = eventsConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(RateLimiterEvent::getCreationTime))
            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
        return new RateLimiterEventsEndpointResponse(eventsList);
    }

    @GetMapping(value = "events/{rateLimiterName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RateLimiterEventsEndpointResponse getEventsFilteredByRateLimiterName(
        @PathVariable("rateLimiterName") String rateLimiterName) {
        List<RateLimiterEventDTO> eventsList = eventsConsumerRegistry
            .getEventConsumer(rateLimiterName).getBufferedEvents()
            .filter(event -> event.getRateLimiterName().equals(rateLimiterName))
            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
        return new RateLimiterEventsEndpointResponse(eventsList);
    }

    @GetMapping(value = "events/{rateLimiterName}/{eventType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RateLimiterEventsEndpointResponse getEventsFilteredByRateLimiterNameAndEventType(
        @PathVariable("rateLimiterName") String rateLimiterName,
        @PathVariable("eventType") String eventType) {
        RateLimiterEvent.Type targetType = RateLimiterEvent.Type.valueOf(eventType.toUpperCase());
        List<RateLimiterEventDTO> eventsList = eventsConsumerRegistry
            .getEventConsumer(rateLimiterName).getBufferedEvents()
            .filter(event -> event.getRateLimiterName().equals(rateLimiterName))
            .filter(event -> event.getEventType() == targetType)
            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
        return new RateLimiterEventsEndpointResponse(eventsList);
    }
}
