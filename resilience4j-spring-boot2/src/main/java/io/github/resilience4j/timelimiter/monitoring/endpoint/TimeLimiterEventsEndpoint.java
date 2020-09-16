/*
 * Copyright 2020 Ingyu Hwang
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

package io.github.resilience4j.timelimiter.monitoring.endpoint;

import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventDTO;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse;
import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Endpoint(id = "timelimiterevents")
public class TimeLimiterEventsEndpoint {

    private final EventConsumerRegistry<TimeLimiterEvent> eventsConsumerRegistry;

    public TimeLimiterEventsEndpoint(EventConsumerRegistry<TimeLimiterEvent> eventsConsumerRegistry) {
        this.eventsConsumerRegistry = eventsConsumerRegistry;
    }

    @ReadOperation
    public TimeLimiterEventsEndpointResponse getAllTimeLimiterEvents() {
        return new TimeLimiterEventsEndpointResponse(eventsConsumerRegistry.getAllEventConsumer().stream()
                .flatMap(CircularEventConsumer::getBufferedEventsStream)
                .sorted(Comparator.comparing(TimeLimiterEvent::getCreationTime))
                .map(TimeLimiterEventDTO::createTimeLimiterEventDTO)
                .collect(Collectors.toList()));
    }

    @ReadOperation
    public TimeLimiterEventsEndpointResponse getEventsFilteredByTimeLimiterName(@Selector String name) {
        return new TimeLimiterEventsEndpointResponse(getTimeLimiterEvents(name).stream()
                .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).collect(Collectors.toList()));
    }

    @ReadOperation
    public TimeLimiterEventsEndpointResponse getEventsFilteredByTimeLimiterNameAndEventType(@Selector String name,
                                                                                            @Selector String eventType) {
        TimeLimiterEvent.Type targetType = TimeLimiterEvent.Type.valueOf(eventType.toUpperCase());
        return new TimeLimiterEventsEndpointResponse(getTimeLimiterEvents(name).stream()
                .filter(event -> event.getEventType() == targetType)
                .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).collect(Collectors.toList()));
    }

    private List<TimeLimiterEvent> getTimeLimiterEvents(String name) {
        CircularEventConsumer<TimeLimiterEvent> eventConsumer = eventsConsumerRegistry.getEventConsumer(name);
        if(eventConsumer != null){
            return eventConsumer.getBufferedEventsStream()
                    .filter(event -> event.getTimeLimiterName().equals(name))
                    .collect(Collectors.toList());
        }else{
            return Collections.emptyList();
        }
    }

}
