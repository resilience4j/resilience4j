/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry.monitoring.endpoint;


import java.util.Comparator;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import io.github.resilience4j.consumer.CircularEventConsumer;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.retry.event.RetryEvent;


/**
 * rest api endpoint to retrieve retry events
 */
@Controller
@RequestMapping(value = "retries/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
public class RetryEventsEndpoint {

	private final EventConsumerRegistry<RetryEvent> eventConsumerRegistry;

	public RetryEventsEndpoint(EventConsumerRegistry<RetryEvent> eventConsumerRegistry) {
		this.eventConsumerRegistry = eventConsumerRegistry;
	}

	@RequestMapping(value = "events", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public RetryEventsEndpointResponse getAllRetryEvenets() {
		return new RetryEventsEndpointResponse(eventConsumerRegistry.getAllEventConsumer()
				.flatMap(CircularEventConsumer::getBufferedEvents)
				.sorted(Comparator.comparing(RetryEvent::getCreationTime))
				.map(RetryEventDTOFactory::createRetryEventDTO).toJavaList());
	}

	@RequestMapping(value = "events/{name}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public RetryEventsEndpointResponse getEventsFilteredByRetryrName(@PathVariable("name") String name) {
		return new RetryEventsEndpointResponse(eventConsumerRegistry.getEventConsumer(name).getBufferedEvents()
				.filter(event -> event.getName().equals(name))
				.map(RetryEventDTOFactory::createRetryEventDTO).toJavaList());
	}

	@RequestMapping(value = "events/{name}/{eventType}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public RetryEventsEndpointResponse getEventsFilteredByRetryNameAndEventType(@PathVariable("name") String name,
	                                                                            @PathVariable("eventType") String eventType) {
		return new RetryEventsEndpointResponse(eventConsumerRegistry.getEventConsumer(name).getBufferedEvents()
				.filter(event -> event.getName().equals(name))
				.filter(event -> event.getEventType() == RetryEvent.Type.valueOf(eventType.toUpperCase()))
				.map(RetryEventDTOFactory::createRetryEventDTO).toJavaList());
	}
}
