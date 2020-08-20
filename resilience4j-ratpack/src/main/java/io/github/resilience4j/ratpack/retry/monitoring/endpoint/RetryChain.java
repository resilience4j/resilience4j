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

package io.github.resilience4j.ratpack.retry.monitoring.endpoint;

import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventDTO;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventDTOFactory;
import io.github.resilience4j.common.retry.monitoring.endpoint.RetryEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratpack.Resilience4jConfig;
import io.github.resilience4j.reactor.adapter.ReactorAdapter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.retry.event.RetryEvent;
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
public class RetryChain implements Action<Chain> {

    private final EventConsumerRegistry<RetryEvent> eventConsumerRegistry;
    private final RetryRegistry retryRegistry;

    @Inject
    public RetryChain(EventConsumerRegistry<RetryEvent> eventConsumerRegistry,
        RetryRegistry retryRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
        this.retryRegistry = retryRegistry;
    }

    @Override
    public void execute(Chain chain) throws Exception {
        String prefix = chain.getRegistry().get(Resilience4jConfig.class).getEndpoints().getRetry()
            .getPath();
        chain.prefix(prefix, chain1 -> {
            chain1.get("events", ctx ->
                Promise.<RetryEventsEndpointResponse>async(d -> {
                    List<RetryEventDTO> eventsList = eventConsumerRegistry.getAllEventConsumer()
                        .flatMap(CircularEventConsumer::getBufferedEvents)
                        .sorted(Comparator.comparing(RetryEvent::getCreationTime))
                        .map(RetryEventDTOFactory::createRetryEventDTO).toJavaList();
                    d.success(new RetryEventsEndpointResponse(eventsList));
                }).then(r -> ctx.render(Jackson.json(r)))
            );
            chain1.get("stream/events", ctx -> {
                Flux<RetryEvent> eventStreams = Flux.fromIterable(retryRegistry.getAllRetries())
                    .flatMap(retry -> ReactorAdapter.toFlux(retry.getEventPublisher()));
                Function<RetryEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(RetryEventDTOFactory.createRetryEventDTO(r));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(eventStreams,
                        e -> e.id(RetryEvent::getName).event(c -> c.getEventType().name())
                            .data(data));
                ctx.render(events);
            });
            chain1.get("events/:name", ctx -> {
                    String retryName = ctx.getPathTokens().get("name");
                    Promise.<RetryEventsEndpointResponse>async(d -> {
                        List<RetryEventDTO> eventsList = eventConsumerRegistry
                            .getEventConsumer(retryName)
                            .getBufferedEvents()
                            .sorted(Comparator.comparing(RetryEvent::getCreationTime))
                            .map(RetryEventDTOFactory::createRetryEventDTO).toJavaList();
                        d.success(new RetryEventsEndpointResponse(eventsList));
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name", ctx -> {
                String rateLimiterName = ctx.getPathTokens().get("name");
                Retry retry = retryRegistry.getAllRetries().stream()
                    .filter(rL -> rL.getName().equals(rateLimiterName))
                    .findAny()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            String.format("rate limiter with name %s not found", rateLimiterName)));
                Function<RetryEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(RetryEventDTOFactory.createRetryEventDTO(r));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(ReactorAdapter.toFlux(retry.getEventPublisher()),
                        e -> e.id(RetryEvent::getName).event(c -> c.getEventType().name())
                            .data(data));
                ctx.render(events);
            });
            chain1.get("events/:name/:type", ctx -> {
                    String retryName = ctx.getPathTokens().get("name");
                    String eventType = ctx.getPathTokens().get("type");
                    Promise.<RetryEventsEndpointResponse>async(d -> {
                        List<RetryEventDTO> eventsList = eventConsumerRegistry
                            .getEventConsumer(retryName)
                            .getBufferedEvents()
                            .sorted(Comparator.comparing(RetryEvent::getCreationTime))
                            .filter(event -> event.getEventType() == RetryEvent.Type
                                .valueOf(eventType.toUpperCase()))
                            .map(RetryEventDTOFactory::createRetryEventDTO).toJavaList();
                        d.success(new RetryEventsEndpointResponse(eventsList));
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name/:type", ctx -> {
                String retryName = ctx.getPathTokens().get("name");
                String eventType = ctx.getPathTokens().get("type");
                Retry retry = retryRegistry.getAllRetries().stream()
                    .filter(rL -> rL.getName().equals(retryName))
                    .findAny()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            String.format("rate limiter with name %s not found", retryName)));
                Flux<RetryEvent> eventStream = ReactorAdapter.toFlux(retry.getEventPublisher())
                    .filter(event -> event.getEventType() == RetryEvent.Type
                        .valueOf(eventType.toUpperCase()));
                Function<RetryEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(RetryEventDTOFactory.createRetryEventDTO(r));
                ServerSentEvents events = ServerSentEvents.serverSentEvents(eventStream,
                    e -> e.id(RetryEvent::getName).event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
        });
    }

}
