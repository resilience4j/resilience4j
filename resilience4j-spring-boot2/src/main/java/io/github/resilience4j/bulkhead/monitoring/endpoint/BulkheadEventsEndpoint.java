/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.monitoring.endpoint;

import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTOFactory;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.vavr.collection.List;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.Comparator;

@Endpoint(id = "bulkheadevents")
public class BulkheadEventsEndpoint {

    private final EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry;

    public BulkheadEventsEndpoint(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
    }

    @ReadOperation
    public BulkheadEventsEndpointResponse getAllBulkheadEvents() {
        java.util.List<BulkheadEventDTO> response = eventConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(BulkheadEvent::getCreationTime))
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @ReadOperation
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadName(
        @Selector String bulkheadName) {
        java.util.List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName)
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @ReadOperation
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadNameAndEventType(
        @Selector String bulkheadName, @Selector String eventType) {
        java.util.List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName)
            .filter(event -> event.getEventType() == BulkheadEvent.Type
                .valueOf(eventType.toUpperCase()))
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    private List<BulkheadEvent> getBulkheadEvent(String bulkheadName) {
        CircularEventConsumer<BulkheadEvent> eventConsumer = eventConsumerRegistry
            .getEventConsumer(bulkheadName);
        if (eventConsumer == null) {
            CircularEventConsumer<BulkheadEvent> threadPoolEventConsumer = eventConsumerRegistry
                .getEventConsumer(
                    String.join("-", ThreadPoolBulkhead.class.getSimpleName(), bulkheadName));
            if (threadPoolEventConsumer != null) {
                return threadPoolEventConsumer.getBufferedEvents()
                    .filter(event -> event.getBulkheadName().equals(bulkheadName));
            } else {
                return List.empty();
            }
        } else {
            return eventConsumer.getBufferedEvents()
                .filter(event -> event.getBulkheadName().equals(bulkheadName));
        }
    }
}
