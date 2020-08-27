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

package io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTOFactory;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerHystrixStreamEventsDTO;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.ratpack.Resilience4jConfig;
import io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.metrics.CircuitBreakerMetricsDTO;
import io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.states.CircuitBreakerStateDTO;
import io.github.resilience4j.ratpack.circuitbreaker.monitoring.endpoint.states.CircuitBreakerStatesEndpointResponse;
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
import java.util.stream.Collectors;

/**
 *<p>
 *  In General, Provides event and stream event endpoints for circuitbreaker events.
 *</p>
 * <p>
 *  The following endpoints are automatically generated and Circuit breake states can be obtained as
 *  curl -v http://localhost:5050/circuitbreaker/states
 *  curl -v http://localhost:5050/circuitbreaker/states/{circuitbreakername}
 * </p>
 * <p>
 *  The following endpoints are automatically generated and events are produced as Server Sent Event(SSE)
 *  curl -vv http://localhost:5050/circuitbreaker/stream/events
 *  curl -vv http://localhost:5050/circuitbreaker/stream/events/{circuitbreakername}
 *  curl -vv http://localhost:5050/circuitbreaker/stream/events/{circuitbreakername}/{errorType}
 *  curl -vv http://localhost:5050/circuitbreaker/hystrixStream/events
 *  curl -vv http://localhost:8090/circuitbreaker/hystrixStream/events/{circuitbreakername}
 *  curl -vv http://localhost:8090/circuitbreaker/hystrixStream/events/{circuitbreakername}/{errorType}
 * <p>
 */
public class CircuitBreakerChain implements Action<Chain> {

    private final EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Inject
    public CircuitBreakerChain(EventConsumerRegistry<CircuitBreakerEvent> eventConsumerRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public void execute(Chain chain) throws Exception {
        String prefix = chain.getRegistry().get(Resilience4jConfig.class).getEndpoints()
            .getCircuitbreaker().getPath();
        chain.prefix(prefix, chain1 -> {
            chain1.get("states/:name", ctx -> {
                String circuitBreakerName = ctx.getPathTokens().get("name");
                Promise.<CircuitBreakerStatesEndpointResponse>async(d -> {
                    CircuitBreakerStatesEndpointResponse response = new CircuitBreakerStatesEndpointResponse(
                        circuitBreakerRegistry
                            .getAllCircuitBreakers().stream()
                            .filter(c -> c.getName().equals(circuitBreakerName))
                            .map(c -> new CircuitBreakerStateDTO(c.getName(), c.getState(),
                                new CircuitBreakerMetricsDTO(c.getMetrics())))
                            .collect(Collectors.toList())
                    );
                    d.success(response);
                }).then(r -> ctx.render(Jackson.json(r)));
            });
            chain1.get("states", ctx ->
                Promise.<CircuitBreakerStatesEndpointResponse>async(d -> {
                    CircuitBreakerStatesEndpointResponse response = new CircuitBreakerStatesEndpointResponse(
                        circuitBreakerRegistry
                            .getAllCircuitBreakers().stream()
                            .map(c -> new CircuitBreakerStateDTO(c.getName(), c.getState(),
                                new CircuitBreakerMetricsDTO(c.getMetrics())))
                            .collect(Collectors.toList())
                    );
                    d.success(response);
                }).then(r -> ctx.render(Jackson.json(r)))
            );
            chain1.get("events", ctx ->
                Promise.<CircuitBreakerEventsEndpointResponse>async(d -> {
                    CircuitBreakerEventsEndpointResponse response = new CircuitBreakerEventsEndpointResponse(
                        eventConsumerRegistry
                            .getAllEventConsumer()
                            .flatMap(CircularEventConsumer::getBufferedEvents)
                            .sorted(Comparator.comparing(CircuitBreakerEvent::getCreationTime))
                            .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO)
                            .toJavaList());
                    d.success(response);
                }).then(r -> ctx.render(Jackson.json(r)))
            );
            chain1.get("stream/events", ctx -> {
                Flux<CircuitBreakerEvent> eventStreams = Flux.fromIterable(circuitBreakerRegistry
                    .getAllCircuitBreakers())
                    .flatMap(circuitBreaker -> ReactorAdapter
                        .toFlux(circuitBreaker.getEventPublisher()));
                Function<CircuitBreakerEvent, String> data = c -> Jackson
                    .getObjectWriter(chain1.getRegistry()).writeValueAsString(
                        CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(c));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(eventStreams,
                        e -> e.id(CircuitBreakerEvent::getCircuitBreakerName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("hystrixStream/events", ctx -> {
                Seq<Flux<CircuitBreakerEvent>> eventStreams = circuitBreakerRegistry
                    .getAllCircuitBreakers().map(circuitBreaker -> ReactorAdapter
                        .toFlux(circuitBreaker.getEventPublisher()));
                Function<CircuitBreakerEvent, String> data = c -> Jackson.getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(
                        circuitBreakerRegistry.getAllCircuitBreakers()
                            .filter(cb -> cb.getName().equals(c.getCircuitBreakerName()))
                            .map(cb -> new CircuitBreakerHystrixStreamEventsDTO(c,
                                cb.getState(),
                                cb.getMetrics(),
                                cb.getCircuitBreakerConfig()
                            ))
                    );
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(Flux.merge(eventStreams),
                        e -> e.id(CircuitBreakerEvent::getCircuitBreakerName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("events/:name", ctx -> {
                    String circuitBreakerName = ctx.getPathTokens().get("name");
                    Promise.<CircuitBreakerEventsEndpointResponse>async(d -> {
                        CircuitBreakerEventsEndpointResponse response = new CircuitBreakerEventsEndpointResponse(
                            eventConsumerRegistry
                                .getEventConsumer(circuitBreakerName)
                                .getBufferedEvents()
                                .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO)
                                .toJavaList());
                        d.success(response);
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name", ctx -> {
                String circuitBreakerName = ctx.getPathTokens().get("name");
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                    .filter(cb -> cb.getName().equals(circuitBreakerName))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException(String
                        .format("circuit breaker with name %s not found", circuitBreakerName)));
                Function<CircuitBreakerEvent, String> data = c -> Jackson
                    .getObjectWriter(chain1.getRegistry()).writeValueAsString(
                        CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(c));
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(ReactorAdapter.toFlux(circuitBreaker.getEventPublisher()),
                        e -> e.id(CircuitBreakerEvent::getCircuitBreakerName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("hystrixStream/events/:name", ctx -> {
                String circuitBreakerName = ctx.getPathTokens().get("name");
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.getAllCircuitBreakers()
                    .find(cb -> cb.getName().equals(circuitBreakerName))
                    .getOrElseThrow(() -> new IllegalArgumentException(String
                        .format("circuit breaker with name %s not found", circuitBreakerName)));
                Function<CircuitBreakerEvent, String> data = c -> Jackson.getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(
                        new CircuitBreakerHystrixStreamEventsDTO(c,
                            circuitBreaker.getState(),
                            circuitBreaker.getMetrics(),
                            circuitBreaker.getCircuitBreakerConfig()
                        )
                    );
                ServerSentEvents events = ServerSentEvents
                    .serverSentEvents(ReactorAdapter.toFlux(circuitBreaker.getEventPublisher()),
                        e -> e.id(CircuitBreakerEvent::getCircuitBreakerName)
                            .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("events/:name/:type", ctx -> {
                    String circuitBreakerName = ctx.getPathTokens().get("name");
                    String eventType = ctx.getPathTokens().get("type");
                    Promise.<CircuitBreakerEventsEndpointResponse>async(d -> {
                        CircuitBreakerEventsEndpointResponse response = new CircuitBreakerEventsEndpointResponse(
                            eventConsumerRegistry
                                .getEventConsumer(circuitBreakerName)
                                .getBufferedEvents()
                                .filter(event -> event.getEventType() == CircuitBreakerEvent.Type
                                    .valueOf(eventType.toUpperCase()))
                                .map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO)
                                .toJavaList());
                        d.success(response);
                    }).then(r -> ctx.render(Jackson.json(r)));
                }
            );
            chain1.get("stream/events/:name/:type", ctx -> {
                String circuitBreakerName = ctx.getPathTokens().get("name");
                String eventType = ctx.getPathTokens().get("type");
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.getAllCircuitBreakers().stream()
                    .filter(cb -> cb.getName().equals(circuitBreakerName))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException(String
                        .format("circuit breaker with name %s not found", circuitBreakerName)));
                Flux<CircuitBreakerEvent> eventStream = ReactorAdapter
                    .toFlux(circuitBreaker.getEventPublisher())
                    .filter(event -> event.getEventType() == CircuitBreakerEvent.Type
                        .valueOf(eventType.toUpperCase()));
                Function<CircuitBreakerEvent, String> data = c -> Jackson
                    .getObjectWriter(chain1.getRegistry()).writeValueAsString(
                        CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(c));
                ServerSentEvents events = ServerSentEvents.serverSentEvents(eventStream,
                    e -> e.id(CircuitBreakerEvent::getCircuitBreakerName)
                        .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
            chain1.get("hystrixStream/events/:name/:type", ctx -> {
                String circuitBreakerName = ctx.getPathTokens().get("name");
                String eventType = ctx.getPathTokens().get("type");
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.getAllCircuitBreakers()
                    .find(cb -> cb.getName().equals(circuitBreakerName))
                    .getOrElseThrow(() -> new IllegalArgumentException(String
                        .format("circuit breaker with name %s not found", circuitBreakerName)));
                Flux<CircuitBreakerEvent> eventStream = ReactorAdapter
                    .toFlux(circuitBreaker.getEventPublisher())
                    .filter(event -> event.getEventType() == CircuitBreakerEvent.Type
                        .valueOf(eventType.toUpperCase()));
                Function<CircuitBreakerEvent, String> data = c -> Jackson.getObjectWriter(chain1.getRegistry())
                    .writeValueAsString(
                        new CircuitBreakerHystrixStreamEventsDTO(c,
                            circuitBreaker.getState(),
                            circuitBreaker.getMetrics(),
                            circuitBreaker.getCircuitBreakerConfig())
                    );
                ServerSentEvents events = ServerSentEvents.serverSentEvents(eventStream,
                    e -> e.id(CircuitBreakerEvent::getCircuitBreakerName)
                        .event(c -> c.getEventType().name()).data(data));
                ctx.render(events);
            });
        });
    }

}
