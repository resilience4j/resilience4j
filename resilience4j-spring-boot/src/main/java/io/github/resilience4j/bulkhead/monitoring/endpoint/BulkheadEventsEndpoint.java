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

import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTO;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventDTOFactory;
import io.github.resilience4j.common.bulkhead.monitoring.endpoint.BulkheadEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.vavr.collection.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;

@Controller
@RequestMapping(value = "bulkhead/")
public class BulkheadEventsEndpoint {

    private final EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry;

    public BulkheadEventsEndpoint(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
    }

    @GetMapping(value = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BulkheadEventsEndpointResponse getAllBulkheadEvents() {
        java.util.List<BulkheadEventDTO> response = eventConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(BulkheadEvent::getCreationTime))
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @GetMapping(value = "events/{bulkheadName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadName(
        @PathVariable("bulkheadName") String bulkheadName) {
        java.util.List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName)
            .map(BulkheadEventDTOFactory::createBulkheadEventDTO)
            .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @GetMapping(value = "events/{bulkheadName}/{eventType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadNameAndEventType(
        @PathVariable("bulkheadName") String bulkheadName,
        @PathVariable("eventType") String eventType) {
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
        if (eventConsumer != null) {
            return eventConsumer.getBufferedEvents()
                .filter(event -> event.getBulkheadName().equals(bulkheadName));
        } else {
            return List.empty();
        }
    }
}
