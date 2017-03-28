package io.github.resilience4j.circuitbreaker.monitoring.endpoint;


import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;

class CircuitBreakerEventEmitter {

    private final SseEmitter sseEmitter;
    private final Disposable disposable;

    private CircuitBreakerEventEmitter(Flowable<CircuitBreakerEventDTO> eventStream) {
        this.sseEmitter = new SseEmitter();
        this.sseEmitter.onCompletion(this::unsubscribe);
        this.sseEmitter.onTimeout(this::unsubscribe);
        this.disposable = eventStream.subscribe(this::notify,
                        this.sseEmitter::completeWithError,
                        this.sseEmitter::complete);
    }

    static SseEmitter createSseEmitter(Flowable<CircuitBreakerEvent> eventStream) {
        return new CircuitBreakerEventEmitter(eventStream.map(CircuitBreakerEventDTOFactory::createCircuitBreakerEventDTO)).sseEmitter;
    }

    private void notify(CircuitBreakerEventDTO circuitBreakerEvent) throws Exception {
        sseEmitter.send(circuitBreakerEvent, MediaType.APPLICATION_JSON);
    }

    private void unsubscribe() {
        this.disposable.dispose();
    }
}
