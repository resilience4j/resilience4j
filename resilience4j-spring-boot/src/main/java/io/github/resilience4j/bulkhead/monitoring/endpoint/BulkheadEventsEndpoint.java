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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.vavr.collection.List;
import io.vavr.collection.Seq;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.Comparator;

import static io.github.resilience4j.adapter.ReactorAdapter.toFlux;

@Controller
@RequestMapping(value = "bulkhead/")
public class BulkheadEventsEndpoint {

    private static final String MEDIA_TYPE_TEXT_EVENT_STREAM = "text/event-stream";
    private final EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    public BulkheadEventsEndpoint(EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry,
                                  BulkheadRegistry bulkheadRegistry) {
        this.eventConsumerRegistry = eventConsumerRegistry;
        this.bulkheadRegistry = bulkheadRegistry;
    }

    @GetMapping(value = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BulkheadEventsEndpointResponse getAllBulkheadEvents() {
        java.util.List<BulkheadEventDTO> response = eventConsumerRegistry.getAllEventConsumer()
                .flatMap(CircularEventConsumer::getBufferedEvents)
                .sorted(Comparator.comparing(BulkheadEvent::getCreationTime))
                .map(BulkheadEventDTOFactory::createBulkheadEventDTOFactory)
                .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @GetMapping(value = "stream/events", produces = MEDIA_TYPE_TEXT_EVENT_STREAM)
    public SseEmitter getBulkheadEventsStream() {
        Seq<Flux<BulkheadEvent>> eventStreams = bulkheadRegistry.getAllBulkheads()
                .map(bulkhead -> toFlux(bulkhead.getEventPublisher()));

        return BulkheadEventEmitter.createSseEmitter(Flux.merge(eventStreams));
    }

    @GetMapping(value = "events/{bulkheadName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadName(@PathVariable("bulkheadName") String bulkheadName) {
        java.util.List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName)
                .map(BulkheadEventDTOFactory::createBulkheadEventDTOFactory)
                .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @GetMapping(value = "stream/events/{bulkheadName}", produces = MEDIA_TYPE_TEXT_EVENT_STREAM)
    public SseEmitter getEventsStreamFilteredByBulkheadName(@PathVariable("bulkheadName") String bulkheadName) {
        Bulkhead bulkhead = getBulkhead(bulkheadName);

        return BulkheadEventEmitter.createSseEmitter(toFlux(bulkhead.getEventPublisher()));
    }

    @GetMapping(value = "events/{bulkheadName}/{eventType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public BulkheadEventsEndpointResponse getEventsFilteredByBulkheadNameAndEventType(@PathVariable("bulkheadName") String bulkheadName,
                                                                                            @PathVariable("eventType") String eventType) {
        java.util.List<BulkheadEventDTO> response = getBulkheadEvent(bulkheadName)
                .filter(event -> event.getEventType() == BulkheadEvent.Type.valueOf(eventType.toUpperCase()))
                .map(BulkheadEventDTOFactory::createBulkheadEventDTOFactory)
                .toJavaList();

        return new BulkheadEventsEndpointResponse(response);
    }

    @GetMapping(value = "stream/events/{bulkheadName}/{eventType}", produces = MEDIA_TYPE_TEXT_EVENT_STREAM)
    public SseEmitter getEventsStreamFilteredByBulkHeadNameAndEventType(@PathVariable("bulkheadName") String bulkheadName,
                                                                        @PathVariable("eventType") String eventType) {
        Bulkhead bulkhead = getBulkhead(bulkheadName);
        Flux<BulkheadEvent> eventStream = toFlux(bulkhead.getEventPublisher())
                .filter(event -> event.getEventType() == BulkheadEvent.Type.valueOf(eventType.toUpperCase()));

        return BulkheadEventEmitter.createSseEmitter(eventStream);
    }

    private Bulkhead getBulkhead(String bulkheadName) {
        return bulkheadRegistry.getAllBulkheads()
                .find(it -> it.getName().equals(bulkheadName))
                .getOrElseThrow(() ->
                        new IllegalArgumentException(String.format("bulkhead with name %s not found", bulkheadName)));
    }

    private List<BulkheadEvent> getBulkheadEvent(String bulkheadName) {
        CircularEventConsumer<BulkheadEvent> eventConsumer = eventConsumerRegistry.getEventConsumer(bulkheadName);
        if(eventConsumer != null){
            return eventConsumer.getBufferedEvents()
                    .filter(event -> event.getBulkheadName().equals(bulkheadName));
        }else{
            return List.empty();
        }
    }
}
