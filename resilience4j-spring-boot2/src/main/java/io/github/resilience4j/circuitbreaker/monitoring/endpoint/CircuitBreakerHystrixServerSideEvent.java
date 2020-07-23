package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerHystrixStreamEventsDTO;
import io.vavr.collection.Seq;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.function.BiFunction;

import static io.github.resilience4j.reactor.adapter.ReactorAdapter.toFlux;

/**
 * @author vijayram
 */

/**
 * This class is used to produce Circuit breaker events as streams in hystrix like fashion
 * <p>
 * The following endpoints are automatically generated and events are produced as Server Sent Event(SSE)
 * curl -vv http://localhost:8090/actuator/hystrix-stream-circuitbreaker-events
 * curl -vv http://localhost:8090/actuator/hystrix-stream-circuitbreaker-events/{circuitbreakername}
 * curl -vv http://localhost:8090/actuator/hystrix-stream-circuitbreaker-events/{circuitbreakername}/{errorType}
 * <p>
 * <p>
 * Note: This SSE data can be easily mapped to hystrix compatible data format (specific K V pairs)
 * and be used in Turbine or hystrix dashboard or vizceral.
 * <p>
 * This is created as a bridge to support the legacy hystrix eco system of monitoring tools especially for
 * those that are migrating from hystrix to resilence4j to continue to use hystrix eco tools.
 */

@Endpoint(id = "hystrix-stream-circuitbreaker-events")
public class CircuitBreakerHystrixServerSideEvent {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerHystrixServerSideEvent(
        CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @ReadOperation(produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> getAllCircuitBreakerHystrixStreamEvents() {
        Seq<Flux<CircuitBreakerEvent>> eventStreams = circuitBreakerRegistry.getAllCircuitBreakers()
            .map(
                circuitBreaker -> toFlux(circuitBreaker.getEventPublisher())
            );
        Flux<CircuitBreakerEvent> eventStream = Flux.merge(eventStreams);
        BiFunction<CircuitBreakerEvent, CircuitBreaker, String> data = getCircuitBreakerEventStringFunction();
        return eventStream.map(
            cbEvent -> ServerSentEvent.<String>builder()
                .id(cbEvent.getCircuitBreakerName())
                .event(cbEvent.getEventType().name())
                .data(data.apply(cbEvent, getCircuitBreaker(cbEvent.getCircuitBreakerName())))
                .build()
        );
    }

    @ReadOperation(produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> getHystrixStreamEventsFilteredByCircuitBreakerName(
        @Selector String name) {

        CircuitBreaker circuitBreaker = getCircuitBreaker(name);
        Flux<CircuitBreakerEvent> eventStream = toFlux(circuitBreaker.getEventPublisher());
        BiFunction<CircuitBreakerEvent, CircuitBreaker, String> data = getCircuitBreakerEventStringFunction();
        return eventStream.map(
            cbEvent -> ServerSentEvent.<String>builder()
                .id(cbEvent.getCircuitBreakerName())
                .event(cbEvent.getEventType().name())
                .data(data.apply(cbEvent, circuitBreaker))
                .build()
        );
    }

    @ReadOperation(produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> getHystrixStreamEventsFilteredByCircuitBreakerNameAndEventType(
        @Selector String name, @Selector String eventType) {

        CircuitBreaker circuitBreaker = getCircuitBreaker(name);
        Flux<CircuitBreakerEvent> eventStream = toFlux(circuitBreaker.getEventPublisher())
            .filter(
                event -> event.getEventType() == CircuitBreakerEvent.Type.valueOf(eventType.toUpperCase())
            );
        BiFunction<CircuitBreakerEvent, CircuitBreaker, String> data = getCircuitBreakerEventStringFunction();
        return eventStream.map(cbEvent -> ServerSentEvent.<String>builder()
            .id(cbEvent.getCircuitBreakerName())
            .event(cbEvent.getEventType().name())
            .data(data.apply(cbEvent, circuitBreaker))
            .build()
        );
    }

    private BiFunction<CircuitBreakerEvent, CircuitBreaker, String> getCircuitBreakerEventStringFunction() {
        ObjectMapper jsonMapper = new ObjectMapper();
        return (cbEvent, cb) -> {
            try {
                return jsonMapper.writeValueAsString(
                    new CircuitBreakerHystrixStreamEventsDTO(cbEvent,
                        cb.getState(),
                        cb.getMetrics(),
                        cb.getCircuitBreakerConfig()
                    )
                );
            } catch (JsonProcessingException e) {
                /* ignore silently */
            }
            return "";
        };
    }

    private CircuitBreaker getCircuitBreaker(String circuitBreakerName) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        if (circuitBreaker == null && !circuitBreaker.getName().equalsIgnoreCase(circuitBreakerName)) {
            new IllegalArgumentException(String
                .format("circuit breaker with name %s not found", circuitBreakerName));
        }
        return circuitBreaker;
    }

}