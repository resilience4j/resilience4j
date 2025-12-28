/*
 * Copyright 2025 Vijay Ram, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerHystrixStreamEventsDTO;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.function.BiFunction;

import static io.github.resilience4j.reactor.adapter.ReactorAdapter.toFlux;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * This class is used to produce Circuit breaker events as streams in hystrix like fashion
 * <p>
 * The following endpoints are automatically generated and events are produced as Server Sent Event(SSE)
 * curl -vv http://localhost:8090/actuator/hystrixstreamcircuitbreakerevents
 * curl -vv http://localhost:8090/actuator/hystrixstreamcircuitbreakerevents/{circuitbreakername}
 * curl -vv http://localhost:8090/actuator/hystrixstreamcircuitbreakerevents/{circuitbreakername}/{errorType}
 * <p>
 * <p>
 * Note: This SSE data can be easily mapped to hystrix compatible data format (specific K V pairs)
 * and be used in Turbine or hystrix dashboard or vizceral.
 * <p>
 * This is created as a bridge to support the legacy hystrix eco system of monitoring tools especially for
 * those that are migrating from hystrix to resilence4j to continue to use hystrix eco tools.
 *
 * Note:  Please see the example of how to consume SSE event here CircuitBreakerHystrixStreamEventsTest.java
 */

@Endpoint(id = "hystrixstreamcircuitbreakerevents")
public class CircuitBreakerHystrixServerSideEvent {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final JsonMapper jsonMapper = new JsonMapper();

    public CircuitBreakerHystrixServerSideEvent(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @ReadOperation(produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getAllCircuitBreakerHystrixStreamEvents() {
        Flux<CircuitBreakerEvent> eventStreams = Flux.fromIterable(circuitBreakerRegistry.getAllCircuitBreakers())
            .flatMap(
                circuitBreaker -> toFlux(circuitBreaker.getEventPublisher())
            );
        return Flux.merge(publishEvents(eventStreams), getHeartbeatStream());
    }

    @ReadOperation(produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getHystrixStreamEventsFilteredByCircuitBreakerName(
        @Selector String name) {

        CircuitBreaker circuitBreaker = getCircuitBreaker(name);
        Flux<CircuitBreakerEvent> eventStream = toFlux(circuitBreaker.getEventPublisher());
        return Flux.merge(publishEvents(eventStream), getHeartbeatStream());

    }

    @ReadOperation(produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getHystrixStreamEventsFilteredByCircuitBreakerNameAndEventType(
        @Selector String name, @Selector String eventType) {

        CircuitBreaker givenCircuitBreaker = getCircuitBreaker(name);
        Flux<CircuitBreakerEvent> eventStream = toFlux(givenCircuitBreaker.getEventPublisher())
            .filter(
                event -> event.getEventType() == CircuitBreakerEvent.Type.valueOf(eventType.toUpperCase())
            );
        return Flux.merge(publishEvents(eventStream), getHeartbeatStream());

    }

    private BiFunction<CircuitBreakerEvent, CircuitBreaker, String> getCircuitBreakerEventStringFunction() {
        return (cbEvent, cb) -> jsonMapper.writeValueAsString(
                new CircuitBreakerHystrixStreamEventsDTO(cbEvent,
                        cb.getState(),
                        cb.getMetrics(),
                        cb.getCircuitBreakerConfig()
                )
        );
    }

    private CircuitBreaker getCircuitBreaker(String circuitBreakerName) {
        return circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
    }

    private Flux<ServerSentEvent<String>> getHeartbeatStream() {
        return Flux.interval(Duration.ofSeconds(1))
            .map(i -> ServerSentEvent.<String>builder().event("ping").build());
    }

    private Flux<ServerSentEvent<String>> publishEvents(Flux<CircuitBreakerEvent> eventStreams) {
        BiFunction<CircuitBreakerEvent, CircuitBreaker, String> circuitBreakerEventDataFn = getCircuitBreakerEventStringFunction();
        return eventStreams
            .onBackpressureDrop()
            .delayElements(Duration.ofMillis(100))
            .map(cbEvent ->
                ServerSentEvent.<String>builder()
                    .id(cbEvent.getCircuitBreakerName())
                    .event(cbEvent.getEventType().name())
                    .data(circuitBreakerEventDataFn.apply(cbEvent, getCircuitBreaker(cbEvent.getCircuitBreakerName())))
                    .build()
            );
    }
}

