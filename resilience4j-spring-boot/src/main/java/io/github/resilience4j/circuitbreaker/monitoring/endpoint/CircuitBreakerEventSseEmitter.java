package io.github.resilience4j.circuitbreaker.monitoring.endpoint;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.reactivex.disposables.Disposable;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class CircuitBreakerEventSseEmitter {

    private final SseEmitter sseEmitter;
    private final Disposable disposable;

    CircuitBreakerEventSseEmitter(CircuitBreaker circuitBreaker) {
        this.sseEmitter = new SseEmitter();
        this.sseEmitter.onCompletion(this::unsubscribe);
        this.disposable = circuitBreaker.getEventStream()
                .subscribe(this::notify,
                        this.sseEmitter::completeWithError,
                        this.sseEmitter::complete);
    }

    SseEmitter getSseEmitter() {
        return this.sseEmitter;
    }

    private void notify(CircuitBreakerEvent circuitBreakerEvent) throws Exception {
        sseEmitter.send(CircuitBreakerEventDTOFactory.createCircuitBreakerEventDTO(circuitBreakerEvent), MediaType.APPLICATION_JSON);
    }

    private void unsubscribe() {
        this.disposable.dispose();
    }
}