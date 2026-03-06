/*
 * Copyright 2026 Robert Winkler
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

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnResetEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class CircuitBreakerEventsEndpointSnapshotReadTest {

    @Test
    public void shouldUseSnapshotListWhenGettingAllEvents() {
        InMemoryEventConsumerRegistry registry = new InMemoryEventConsumerRegistry();
        ThrowingStreamCircularEventConsumer eventConsumer = new ThrowingStreamCircularEventConsumer();
        eventConsumer.consumeEvent(new CircuitBreakerOnResetEvent("backendA"));
        registry.add("backendA", eventConsumer);
        CircuitBreakerEventsEndpoint endpoint = new CircuitBreakerEventsEndpoint(registry);

        CircuitBreakerEventsEndpointResponse response = endpoint.getAllCircuitBreakerEvents();

        assertThat(response.getCircuitBreakerEvents()).hasSize(1);
        assertThat(eventConsumer.getStreamCalls()).isZero();
    }

    @Test
    public void shouldUseSnapshotListWhenFilteringByName() {
        InMemoryEventConsumerRegistry registry = new InMemoryEventConsumerRegistry();
        ThrowingStreamCircularEventConsumer eventConsumer = new ThrowingStreamCircularEventConsumer();
        eventConsumer.consumeEvent(new CircuitBreakerOnSuccessEvent("backendA", Duration.ofMillis(5)));
        eventConsumer.consumeEvent(new CircuitBreakerOnResetEvent("backendB"));
        registry.add("backendA", eventConsumer);
        CircuitBreakerEventsEndpoint endpoint = new CircuitBreakerEventsEndpoint(registry);

        CircuitBreakerEventsEndpointResponse response =
            endpoint.getEventsFilteredByCircuitBreakerName("backendA");

        assertThat(response.getCircuitBreakerEvents()).hasSize(1);
        assertThat(response.getCircuitBreakerEvents().get(0).getCircuitBreakerName())
            .isEqualTo("backendA");
        assertThat(eventConsumer.getStreamCalls()).isZero();
    }

    @Test
    public void shouldUseSnapshotListWhenFilteringByNameAndType() {
        InMemoryEventConsumerRegistry registry = new InMemoryEventConsumerRegistry();
        ThrowingStreamCircularEventConsumer eventConsumer = new ThrowingStreamCircularEventConsumer();
        eventConsumer.consumeEvent(new CircuitBreakerOnSuccessEvent("backendA", Duration.ofMillis(5)));
        eventConsumer.consumeEvent(new CircuitBreakerOnResetEvent("backendA"));
        registry.add("backendA", eventConsumer);
        CircuitBreakerEventsEndpoint endpoint = new CircuitBreakerEventsEndpoint(registry);

        CircuitBreakerEventsEndpointResponse response =
            endpoint.getEventsFilteredByCircuitBreakerNameAndEventType("backendA", "success");

        assertThat(response.getCircuitBreakerEvents()).hasSize(1);
        assertThat(response.getCircuitBreakerEvents().get(0).getType())
            .isEqualTo(CircuitBreakerEvent.Type.SUCCESS);
        assertThat(eventConsumer.getStreamCalls()).isZero();
    }

    private static class InMemoryEventConsumerRegistry
        implements EventConsumerRegistry<CircuitBreakerEvent> {

        private final Map<String, CircularEventConsumer<CircuitBreakerEvent>> byName = new HashMap<>();

        private final List<CircularEventConsumer<CircuitBreakerEvent>> consumers = new ArrayList<>();

        void add(String name, CircularEventConsumer<CircuitBreakerEvent> eventConsumer) {
            byName.put(name, eventConsumer);
            consumers.add(eventConsumer);
        }

        @Override
        public CircularEventConsumer<CircuitBreakerEvent> createEventConsumer(String id, int bufferSize) {
            ThrowingStreamCircularEventConsumer eventConsumer =
                new ThrowingStreamCircularEventConsumer(bufferSize);
            add(id, eventConsumer);
            return eventConsumer;
        }

        @Override
        public CircularEventConsumer<CircuitBreakerEvent> removeEventConsumer(String id) {
            CircularEventConsumer<CircuitBreakerEvent> removed = byName.remove(id);
            if (removed != null) {
                consumers.remove(removed);
            }
            return removed;
        }

        @Override
        public CircularEventConsumer<CircuitBreakerEvent> getEventConsumer(String id) {
            return byName.get(id);
        }

        @Override
        public List<CircularEventConsumer<CircuitBreakerEvent>> getAllEventConsumer() {
            return List.copyOf(consumers);
        }
    }

    private static class ThrowingStreamCircularEventConsumer
        extends CircularEventConsumer<CircuitBreakerEvent> {

        private int streamCalls;

        ThrowingStreamCircularEventConsumer() {
            this(16);
        }

        ThrowingStreamCircularEventConsumer(int capacity) {
            super(capacity);
        }

        @Override
        public Stream<CircuitBreakerEvent> getBufferedEventsStream() {
            streamCalls++;
            throw new ConcurrentModificationException("live stream should not be used");
        }

        int getStreamCalls() {
            return streamCalls;
        }
    }
}
