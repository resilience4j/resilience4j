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

import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratelimiter.monitoring.model.RateLimiterEventDTO;
import io.github.resilience4j.ratelimiter.monitoring.model.RateLimiterEventsEndpointResponse;
import io.vavr.collection.Seq;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;

import static io.github.resilience4j.adapter.ReactorAdapter.toFlux;

@Controller
@RequestMapping(value = "ratelimiter/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
public class RateLimiterEventsEndpoint {
    private static final String MEDIA_TYPE_TEXT_EVENT_STREAM = "text/event-stream";
    private final EventConsumerRegistry<RateLimiterEvent> eventsConsumerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimiterEventsEndpoint(EventConsumerRegistry<RateLimiterEvent> eventsConsumerRegistry,
                                     RateLimiterRegistry rateLimiterRegistry) {
        this.eventsConsumerRegistry = eventsConsumerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }


    @RequestMapping(value = "events", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RateLimiterEventsEndpointResponse getAllRateLimiterEvents() {
        List<RateLimiterEventDTO> eventsList = eventsConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(RateLimiterEvent::getCreationTime))
            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
        return new RateLimiterEventsEndpointResponse(eventsList);
    }

    @RequestMapping(value = "stream/events", produces = MEDIA_TYPE_TEXT_EVENT_STREAM)
    public SseEmitter getAllRateLimiterEventsStream() {
        Seq<Flux<RateLimiterEvent>> eventStreams = rateLimiterRegistry.getAllRateLimiters()
            .map(rateLimiter -> toFlux(rateLimiter.getEventPublisher()));
        return RateLimiterEventsEmitter.createSseEmitter(Flux.merge(eventStreams));
    }

    @RequestMapping(value = "events/{rateLimiterName}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RateLimiterEventsEndpointResponse getEventsFilteredByRateLimiterName(@PathVariable("rateLimiterName") String rateLimiterName) {
        List<RateLimiterEventDTO> eventsList = eventsConsumerRegistry.getEventConsumer(rateLimiterName).getBufferedEvents()
            .filter(event -> event.getRateLimiterName().equals(rateLimiterName))
            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
        return new RateLimiterEventsEndpointResponse(eventsList);
    }

    @RequestMapping(value = "stream/events/{rateLimiterName}", produces = MEDIA_TYPE_TEXT_EVENT_STREAM)
    public SseEmitter getEventsStreamFilteredByRateLimiterName(@PathVariable("rateLimiterName") String rateLimiterName) {
        RateLimiter rateLimiter = rateLimiterRegistry.getAllRateLimiters()
            .find(rL -> rL.getName().equals(rateLimiterName))
            .getOrElseThrow(() ->
                new IllegalArgumentException(String.format("rate limiter with name %s not found", rateLimiterName)));
        return RateLimiterEventsEmitter.createSseEmitter(toFlux(rateLimiter.getEventPublisher()));
    }

    @RequestMapping(value = "events/{rateLimiterName}/{eventType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RateLimiterEventsEndpointResponse getEventsFilteredByRateLimiterNameAndEventType(@PathVariable("rateLimiterName") String rateLimiterName,
                                                                                            @PathVariable("eventType") String eventType) {
        RateLimiterEvent.Type targetType = RateLimiterEvent.Type.valueOf(eventType.toUpperCase());
        List<RateLimiterEventDTO> eventsList = eventsConsumerRegistry.getEventConsumer(rateLimiterName).getBufferedEvents()
            .filter(event -> event.getRateLimiterName().equals(rateLimiterName))
            .filter(event -> event.getEventType() == targetType)
            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
        return new RateLimiterEventsEndpointResponse(eventsList);
    }

    @RequestMapping(value = "stream/events/{rateLimiterName}/{eventType}", produces = MEDIA_TYPE_TEXT_EVENT_STREAM)
    public SseEmitter getEventsStreamFilteredByRateLimiterNameAndEventType(@PathVariable("rateLimiterName") String rateLimiterName,
                                                                           @PathVariable("eventType") String eventType) {
        RateLimiterEvent.Type targetType = RateLimiterEvent.Type.valueOf(eventType.toUpperCase());
        RateLimiter rateLimiter = rateLimiterRegistry.getAllRateLimiters()
            .find(rL -> rL.getName().equals(rateLimiterName))
            .getOrElseThrow(() ->
                new IllegalArgumentException(String.format("rate limiter with name %s not found", rateLimiterName)));
        Flux<RateLimiterEvent> eventStream = toFlux(rateLimiter.getEventPublisher())
            .filter(event -> event.getEventType() == targetType);
        return RateLimiterEventsEmitter.createSseEmitter(eventStream);
    }
}
