/*
 * Copyright 2025 Vijay Ram, Artur Havliukovskyi
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
package io.github.resilience4j.springboot.circuitbreaker.monitoring.events;


import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.springboot.service.test.DummyService;
import io.github.resilience4j.springboot.service.test.TestApplication;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author vijayram
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = TestApplication.class)
@AutoConfigureWebTestClient(timeout="36000")
@Ignore
public class CircuitBreakerStreamEventsTest {

    public static final String ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS = "/actuator/streamcircuitbreakerevents";
    public static final String ACTUATOR_CIRCUITBREAKEREVENTS = "/actuator/circuitbreakerevents";
    @LocalServerPort
    int randomServerPort;
    @Autowired
    DummyService dummyService;
    @Autowired
    private WebTestClient webTestClient;

    private WebClient webStreamClient;

    @Before
    public void setup(){
        webStreamClient = WebClient.create("http://localhost:" + randomServerPort);
    }

    private final ParameterizedTypeReference<ServerSentEvent<String>> type
        = new ParameterizedTypeReference<ServerSentEvent<String>>() {
    };

    @Test
    public void streamAllEvents() throws IOException, InterruptedException {
        int noOfEvents =2;
        List<ServerSentEvent<String>> noOfEventsFromStream = getServerSentEvents(ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS);
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS);
        publishEvents(noOfEvents);
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsAfter = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS);
        assertThat(circuitBreakerEventsBefore.getCircuitBreakerEvents().size()).isLessThan(circuitBreakerEventsAfter.getCircuitBreakerEvents().size());
        Thread.sleep(1000);
        assertThat(noOfEventsFromStream).hasSize(noOfEvents);
    }

    @Test
    public void streamEventsbyName() throws IOException, InterruptedException {
        int noOfEvents =2;
        List<ServerSentEvent<String>> noOfEventsFromStream = getServerSentEvents(ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS + "/backendA");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        publishEvents(noOfEvents);
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsAfter = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        assertThat(circuitBreakerEventsBefore.getCircuitBreakerEvents().size()).isLessThan(circuitBreakerEventsAfter.getCircuitBreakerEvents().size());
        Thread.sleep(1000);
        assertThat(noOfEventsFromStream).hasSize(noOfEvents);
    }

    @Test
    public void streamEventsbyNameAndType() throws IOException, InterruptedException {
        int noOfSuccessfulEvents =1;
        List<ServerSentEvent<String>> noOfEventsFromStream = getServerSentEvents(ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS + "/backendA/SUCCESS");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        publishEventsWithSuccessAndError();
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsAfter = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        assertThat(circuitBreakerEventsBefore.getCircuitBreakerEvents().size()).isLessThan(circuitBreakerEventsAfter.getCircuitBreakerEvents().size());
        Thread.sleep(1000);
        assertThat(noOfEventsFromStream).hasSize(noOfSuccessfulEvents);
    }

    private List<ServerSentEvent<String>> getServerSentEvents(String s) {
        Flux<ServerSentEvent<String>> circuitBreakerStreamEventsForAfter = circuitBreakerStreamEvents(s);
        List<ServerSentEvent<String>> events = new ArrayList<>();

        circuitBreakerStreamEventsForAfter.subscribe(
            content -> events.add(content),
            error -> System.out.println("Error receiving SSE: {}" + error),
            () -> System.out.println("Completed!!!"));
        return events;
    }

    private CircuitBreakerEventsEndpointResponse circuitBreakerEvents(String s) {
        return this.webTestClient.get().uri(s).exchange()
            .expectStatus().isOk()
            .expectBody(CircuitBreakerEventsEndpointResponse.class)
            .returnResult()
            .getResponseBody();
    }

    private Flux<ServerSentEvent<String>> circuitBreakerStreamEvents(String s) {
        Flux<ServerSentEvent<String>> eventStream = webStreamClient.get()
            .uri(s)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(type)
            .filter(eventData -> !eventData.event().equals("ping"))
            .take(3);
        return eventStream;
    }

    private void publishEvents(int noOfEvents) throws IOException {
        int i =0;
        while( i < noOfEvents){
            dummyService.doSomething(false);
            // The invocation is recorded by the CircuitBreaker as a success.
            i++;
        }
    }

    private void publishEventsWithSuccessAndError() throws IOException {
        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);
    }
}
