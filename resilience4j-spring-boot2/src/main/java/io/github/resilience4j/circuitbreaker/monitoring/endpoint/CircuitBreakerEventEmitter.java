package io.github.resilience4j.circuitbreaker.monitoring.endpoint;


import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class CircuitBreakerEventEmitter {

    private static final Logger LOG = LoggerFactory.getLogger(CircuitBreakerEventEmitter.class);

    private final SseEmitter sseEmitter;
    private final Disposable disposable;

    private CircuitBreakerEventEmitter(Flux<CircuitBreakerEventDTO> eventStream) {
        this.sseEmitter = new SseEmitter();
        this.sseEmitter.onCompletion(this::unsubscribe);
        this.sseEmitter.onTimeout(this::unsubscribe);
        this.disposable = eventStream.subscribe(this::notify,
                        this.sseEmitter::completeWithError,
                        this.sseEmitter::complete);
    }

    public static SseEmitter createSseEmitter(Flux<CircuitBreakerEvent> eventStream) {
        return new CircuitBreakerEventEmitter(eventStream.map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO)).sseEmitter;
    }

    private void notify(CircuitBreakerEventDTO circuitBreakerEvent){
        try {
            sseEmitter.send(circuitBreakerEvent, MediaType.APPLICATION_JSON);
        } catch (IOException e) {
            LOG.warn("Failed to send circuitbreaker event", e);
        }
    }

    private void unsubscribe() {
        this.disposable.dispose();
    }
}
