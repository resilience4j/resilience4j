package org.springframework.web.servlet.mvc.method.annotation;

import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateRunnable;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.ERROR;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.IGNORED_ERROR;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.NOT_PERMITTED;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.STATE_TRANSITION;
import static io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent.Type.SUCCESS;
import static io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventEmitter.createSseEmitter;
import static java.util.stream.Collectors.toList;

import org.junit.Test;
import org.springframework.http.MediaType;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.monitoring.endpoint.CircuitBreakerEventDTO;
import io.vavr.control.Try;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

/**
 * @author bstorozhuk
 */
public class CircuitBreakerEventEmitterTest {

    @Test
    public void testEmitter() throws IOException {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .ringBufferSizeInClosedState(3)
            .ringBufferSizeInHalfOpenState(2)
            .failureRateThreshold(66)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .recordFailure(e -> !(e instanceof IllegalArgumentException))
            .build();

        CircuitBreaker circuitBreaker = CircuitBreakerRegistry.ofDefaults().circuitBreaker("test", config);
        Runnable run = decorateRunnable(circuitBreaker, () -> System.out.println("."));
        Runnable fail = decorateRunnable(circuitBreaker, () -> {
            throw new ConcurrentModificationException();
        });
        Runnable ignore = decorateRunnable(circuitBreaker, () -> {
            throw new IllegalArgumentException();
        });

        SseEmitter sseEmitter = createSseEmitter(circuitBreaker.getEventStream());
        TestHandler handler = new TestHandler();
        sseEmitter.initialize(handler);

        exec(run, 2);
        exec(ignore, 1);
        exec(fail, 3);
        sseEmitter.complete();
        then(handler.isCompleted).isTrue();

        exec(run, 2);

        List<CircuitBreakerEvent.Type> events = handler.events.stream()
            .map(CircuitBreakerEventDTO::getType)
            .collect(toList());

        then(events).containsExactly(SUCCESS, SUCCESS, IGNORED_ERROR, ERROR, ERROR, STATE_TRANSITION, NOT_PERMITTED);
    }

    private void exec(Runnable runnable, int times) {
        for (int i = 0; i < times; i++) {
            Try.runRunnable(runnable)
                .onFailure(e -> System.out.println(e.getMessage()));
        }
    }

    private static class TestHandler implements ResponseBodyEmitter.Handler {
        List<CircuitBreakerEventDTO> events = new ArrayList<>();
        boolean isCompleted = false;
        private Runnable callback;

        @Override public void send(Object data, MediaType mediaType) throws IOException {
            if (APPLICATION_JSON == mediaType && data instanceof CircuitBreakerEventDTO) {
                events.add(((CircuitBreakerEventDTO) data));
            }
        }

        @Override public void complete() {
            isCompleted = true;
            callback.run();
        }

        @Override
        public void completeWithError(Throwable failure) {
            System.out.println("E");
        }

        @Override
        public void onTimeout(Runnable callback) {
            System.out.println("T");
        }

        @Override
        public void onCompletion(Runnable callback) {
            this.callback = callback;
        }
    }

}