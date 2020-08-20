/*
 * Copyright 2017 Dan Maas
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

package io.github.resilience4j.ratpack.ratelimiter.monitoring.endpoint;

import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventDTO;
import io.github.resilience4j.common.ratelimiter.monitoring.endpoint.RateLimiterEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.event.RateLimiterEvent;
import io.github.resilience4j.ratpack.Resilience4jConfig;
import io.github.resilience4j.reactor.adapter.ReactorAdapter;
import ratpack.exec.Promise;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Chain;
import ratpack.jackson.Jackson;
import ratpack.sse.ServerSentEvents;
import reactor.core.publisher.Flux;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.List;

/**
 * Provides event and stream event endpoints for circuitbreaker events.
 */
public class RateLimiterChain implements Action<Chain> {

    private final EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry;
    private final RateLimiterRegistry rateLimiterRegistry;

    @Inject
    public RateLimiterChain(EventConsumerRegistry<RateLimiterEvent> eventConsumerRegistry,
        RateLimiterRegistry rateLimiterRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public void execute(Chain chain) throws Exception {
        String prefix = chain.getRegistry().get(Resilience4jConfig.class).getEndpoints()
            .getRatelimiter().getPath();
        chain.prefix(prefix, chain1 -> {
            chain1.get("events", ctx ->
                Promise.<RateLimiterEventsEndpointResponse>async(d -> {
                    List<RateLimiterEventDTO> eventsList = eventConsumerRegistry
                        .getAllEventConsumer()
                        .flatMap(CircularEventConsumer::getBufferedEvents)
                        .sorted(Comparator.comparing(RateLimiterEvent::getCreationTime))
                        .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
                    d.success(new RateLimiterEventsEndpointResponse(eventsList));
                }).then(r -> ctx.render(Jackson.json(r)))
            );
            chain1.get("stream/events", ctx -> {
                Flux<RateLimiterEvent> eventStreams = Flux.fromIterable(rateLimiterRegistry.getAllRateLimiters())
                    .flatMap(rateLimiter -> ReactorAdapter.toFlux(rateLimiter.getEventPublisher()));
                Function<RateLimiterEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(RateLimiterEventDTO.createRateLimiterEventDTO(r));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(eventStreams,
                        e -> e.id(RateLimiterEvent::getRateLimiterName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("events/:name", ctx -> {
                    String rateLimiterName = ctx.getPathTokens().get("name");
                    Promise.<RateLimiterEventsEndpointResponse>async(d -> {
                        List<RateLimiterEventDTO> eventsList = eventConsumerRegistry
                            .getEventConsumer(rateLimiterName)
                            .getBufferedEvents()
                            .sorted(Comparator.comparing(RateLimiterEvent::getCreationTime))
                            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
                        d.success(new RateLimiterEventsEndpointResponse(eventsList));
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name", ctx -> {
                String rateLimiterName = ctx.getPathTokens().get("name");
                RateLimiter rateLimiter = rateLimiterRegistry.getAllRateLimiters().stream()
                    .filter(rL -> rL.getName().equals(rateLimiterName))
                    .findAny()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            String.format("rate limiter with name %s not found", rateLimiterName)));
                Function<RateLimiterEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(RateLimiterEventDTO.createRateLimiterEventDTO(r));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(ReactorAdapter.toFlux(rateLimiter.getEventPublisher()),
                        e -> e.id(RateLimiterEvent::getRateLimiterName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("events/:name/:type", ctx -> {
                    String rateLimiterName = ctx.getPathTokens().get("name");
                    String eventType = ctx.getPathTokens().get("type");
                    Promise.<RateLimiterEventsEndpointResponse>async(d -> {
                        List<RateLimiterEventDTO> eventsList = eventConsumerRegistry
                            .getEventConsumer(rateLimiterName)
                            .getBufferedEvents()
                            .sorted(Comparator.comparing(RateLimiterEvent::getCreationTime))
                            .filter(event -> event.getEventType() == RateLimiterEvent.Type
                                .valueOf(eventType.toUpperCase()))
                            .map(RateLimiterEventDTO::createRateLimiterEventDTO).toJavaList();
                        d.success(new RateLimiterEventsEndpointResponse(eventsList));
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name/:type", ctx -> {
                String rateLimiterName = ctx.getPathTokens().get("name");
                String eventType = ctx.getPathTokens().get("type");
                RateLimiter rateLimiter = rateLimiterRegistry.getAllRateLimiters().stream()
                    .filter(rL -> rL.getName().equals(rateLimiterName))
                    .findAny()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            String.format("rate limiter with name %s not found", rateLimiterName)));
                Flux<RateLimiterEvent> eventStream = ReactorAdapter
                    .toFlux(rateLimiter.getEventPublisher())
                    .filter(event -> event.getEventType() == RateLimiterEvent.Type
                        .valueOf(eventType.toUpperCase()));
                Function<RateLimiterEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(RateLimiterEventDTO.createRateLimiterEventDTO(r));
                ServerSentEvents events = ServerSentEvents.serverSentEvents(eventStream,
                    e -> e.id(RateLimiterEvent::getRateLimiterName)
                        .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
        });
    }

}
