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
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Endpoint(id = "bulkheadevents")
public class BulkheadEventsEndpoint {

    private final EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry;

    public BulkheadEventsEndpoint(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
    }

    @ReadOperation
    public BulkheadEventsEndpointResponse getAllBulkheadEvents() {
        List<BulkheadEventDTO> response = eventConsumerRegistry.getAllEventConsumer().stream()
            .flatMap(CircularEventConsumer::getBufferedEventsStream)
            .sorted(Comparator.comparing(BulkheadEvent::getCreationTime))
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .collect(Collectors.toList());
        return new BulkheadEventsEndpointResponse(response);
    }

    @ReadOperation
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadName(
        @Selector String bulkheadName) {
        List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName).stream()
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .collect(Collectors.toList());

        return new BulkheadEventsEndpointResponse(response);
    }

    @ReadOperation
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadNameAndEventType(
        @Selector String bulkheadName, @Selector String eventType) {
        List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName).stream()
            .filter(event -> event.getEventType() == BulkheadEvent.Type
                .valueOf(eventType.toUpperCase()))
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .collect(Collectors.toList());

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
                return threadPoolEventConsumer.getBufferedEventsStream()
                    .filter(event -> event.getBulkheadName().equals(bulkheadName))
                    .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        } else {
            return eventConsumer.getBufferedEventsStream()
                .filter(event -> event.getBulkheadName().equals(bulkheadName))
                .collect(Collectors.toList());
        }
    }
}
