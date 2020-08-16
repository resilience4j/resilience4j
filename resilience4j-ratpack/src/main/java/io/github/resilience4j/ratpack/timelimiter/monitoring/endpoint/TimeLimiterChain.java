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

package io.github.resilience4j.ratpack.timelimiter.monitoring.endpoint;

import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventDTO;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratpack.Resilience4jConfig;
import io.github.resilience4j.reactor.adapter.ReactorAdapter;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
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
 * Provides event and stream event endpoints for timelimiter events.
 */
public class TimeLimiterChain implements Action<Chain> {

    private final EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry;
    private final TimeLimiterRegistry timeLimiterRegistry;

    @Inject
    public TimeLimiterChain(EventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry,
                            TimeLimiterRegistry timeLimiterRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
        this.timeLimiterRegistry = timeLimiterRegistry;
    }

    @Override
    public void execute(Chain chain) throws Exception {
        String prefix = chain.getRegistry().get(Resilience4jConfig.class).getEndpoints()
            .getTimelimiter().getPath();
        chain.prefix(prefix, chain1 -> {
            chain1.get("events", ctx ->
                Promise.<TimeLimiterEventsEndpointResponse>async(d -> {
                    List<TimeLimiterEventDTO> eventsList = eventConsumerRegistry
                        .getAllEventConsumer()
                        .flatMap(CircularEventConsumer::getBufferedEvents)
                        .sorted(Comparator.comparing(TimeLimiterEvent::getCreationTime))
                        .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).toJavaList();
                    d.success(new TimeLimiterEventsEndpointResponse(eventsList));
                }).then(r -> ctx.render(Jackson.json(r)))
            );
            chain1.get("stream/events", ctx -> {
                Flux<TimeLimiterEvent> eventStreams = Flux.fromIterable(timeLimiterRegistry.getAllTimeLimiters())
                    .flatMap(timeLimiter -> ReactorAdapter.toFlux(timeLimiter.getEventPublisher()));
                Function<TimeLimiterEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(TimeLimiterEventDTO.createTimeLimiterEventDTO(r));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(eventStreams,
                        e -> e.id(TimeLimiterEvent::getTimeLimiterName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("events/:name", ctx -> {
                    String timeLimiterName = ctx.getPathTokens().get("name");
                    Promise.<TimeLimiterEventsEndpointResponse>async(d -> {
                        List<TimeLimiterEventDTO> eventsList = eventConsumerRegistry
                            .getEventConsumer(timeLimiterName)
                            .getBufferedEvents()
                            .sorted(Comparator.comparing(TimeLimiterEvent::getCreationTime))
                            .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).toJavaList();
                        d.success(new TimeLimiterEventsEndpointResponse(eventsList));
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name", ctx -> {
                String timeLimiterName = ctx.getPathTokens().get("name");
                TimeLimiter timeLimiter = timeLimiterRegistry.getAllTimeLimiters().stream()
                    .filter(tL -> tL.getName().equals(timeLimiterName))
                    .findAny()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            String.format("time limiter with name %s not found", timeLimiterName)));
                Function<TimeLimiterEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(TimeLimiterEventDTO.createTimeLimiterEventDTO(r));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(ReactorAdapter.toFlux(timeLimiter.getEventPublisher()),
                        e -> e.id(TimeLimiterEvent::getTimeLimiterName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("events/:name/:type", ctx -> {
                    String timeLimiterName = ctx.getPathTokens().get("name");
                    String eventType = ctx.getPathTokens().get("type");
                    Promise.<TimeLimiterEventsEndpointResponse>async(d -> {
                        List<TimeLimiterEventDTO> eventsList = eventConsumerRegistry
                            .getEventConsumer(timeLimiterName)
                            .getBufferedEvents()
                            .sorted(Comparator.comparing(TimeLimiterEvent::getCreationTime))
                            .filter(event -> event.getEventType() == TimeLimiterEvent.Type
                                .valueOf(eventType.toUpperCase()))
                            .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).toJavaList();
                        d.success(new TimeLimiterEventsEndpointResponse(eventsList));
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name/:type", ctx -> {
                String timeLimiterName = ctx.getPathTokens().get("name");
                String eventType = ctx.getPathTokens().get("type");
                TimeLimiter timeLimiter = timeLimiterRegistry.getAllTimeLimiters().stream()
                    .filter(rL -> rL.getName().equals(timeLimiterName))
                    .findAny()
                    .orElseThrow(() ->
                        new IllegalArgumentException(
                            String.format("time limiter with name %s not found", timeLimiterName)));
                Flux<TimeLimiterEvent> eventStream = ReactorAdapter
                    .toFlux(timeLimiter.getEventPublisher())
                    .filter(event -> event.getEventType() == TimeLimiterEvent.Type
                        .valueOf(eventType.toUpperCase()));
                Function<TimeLimiterEvent, String> data = r -> Jackson
                    .getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(TimeLimiterEventDTO.createTimeLimiterEventDTO(r));
                ServerSentEvents events = ServerSentEvents.serverSentEvents(eventStream,
                    e -> e.id(TimeLimiterEvent::getTimeLimiterName)
                        .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
        });
    }

}
