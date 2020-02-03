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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping(value = "timelimiter/")
public class TimeLimiterEventsEndpoint {

    private final EventConsumerRegistry<TimeLimiterEvent> eventsConsumerRegistry;

    public TimeLimiterEventsEndpoint(EventConsumerRegistry<TimeLimiterEvent> eventsConsumerRegistry) {
        this.eventsConsumerRegistry = eventsConsumerRegistry;
    }

    @GetMapping(value = "events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TimeLimiterEventsEndpointResponse getAllTimeLimiterEvents() {
        List<TimeLimiterEventDTO> eventsList = eventsConsumerRegistry.getAllEventConsumer()
            .flatMap(CircularEventConsumer::getBufferedEvents)
            .sorted(Comparator.comparing(TimeLimiterEvent::getCreationTime))
            .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).toJavaList();
        return new TimeLimiterEventsEndpointResponse(eventsList);
    }

    @GetMapping(value = "events/{timeLimiterName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TimeLimiterEventsEndpointResponse getEventsFilteredByTimeLimiterName(
        @PathVariable("timeLimiterName") String timeLimiterName) {
        List<TimeLimiterEventDTO> eventsList = eventsConsumerRegistry.getEventConsumer(timeLimiterName).getBufferedEvents()
            .filter(event -> event.getTimeLimiterName().equals(timeLimiterName))
            .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).toJavaList();
        return new TimeLimiterEventsEndpointResponse(eventsList);
    }

    @GetMapping(value = "events/{timeLimiterName}/{eventType}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public TimeLimiterEventsEndpointResponse getEventsFilteredByTimeLimiterNameAndEventType(
        @PathVariable("timeLimiterName") String timeLimiterName,
        @PathVariable("eventType") String eventType) {
        TimeLimiterEvent.Type targetType = TimeLimiterEvent.Type.valueOf(eventType.toUpperCase());
        List<TimeLimiterEventDTO> eventsList = eventsConsumerRegistry.getEventConsumer(timeLimiterName)
            .getBufferedEvents()
            .filter(event -> event.getTimeLimiterName().equals(timeLimiterName))
            .filter(event -> event.getEventType() == targetType)
            .map(TimeLimiterEventDTO::createTimeLimiterEventDTO).toJavaList();
        return new TimeLimiterEventsEndpointResponse(eventsList);
    }
}
