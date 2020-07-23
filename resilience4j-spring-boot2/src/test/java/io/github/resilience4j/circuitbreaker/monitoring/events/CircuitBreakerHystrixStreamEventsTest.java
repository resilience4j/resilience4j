package io.github.resilience4j.circuitbreaker.monitoring.events;


import io.github.resilience4j.common.circuitbreaker.monitoring.endpoint.CircuitBreakerEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
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


/**
 * @author vijayram
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = TestApplication.class)
public class CircuitBreakerHystrixStreamEventsTest {

    public static final String ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS = "/actuator/hystrix-stream-circuitbreaker-events";
    public static final String ACTUATOR_CIRCUITBREAKEREVENTS = "/actuator/circuitbreakerevents";

    @Autowired
    private WebTestClient webTestClient;

    @LocalServerPort
    int randomServerPort;

    @Autowired
    DummyService dummyService;

    @Test
    public void streamAllEvents() throws IOException, InterruptedException {
        List<ServerSentEvent<String>> events = getServerSentEvents(ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS);
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS);
        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);

        CircuitBreakerEventsEndpointResponse circuitBreakerEventsAfter = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS);
        assert (circuitBreakerEventsBefore.getCircuitBreakerEvents().size() < circuitBreakerEventsAfter.getCircuitBreakerEvents().size());
        assert circuitBreakerEventsAfter.getCircuitBreakerEvents().size() == 2;
        Thread.sleep(1000); // for webClient to complete the subscribe operation
        assert (events.size() == 2);
    }

    @Test
    public void streamEventsbyName() throws IOException, InterruptedException {
        List<ServerSentEvent<String>> events = getServerSentEvents(ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS + "/backendA");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);
        Thread.sleep(1000); // sleep is needed to record the event
        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsAfter = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        Thread.sleep(1000); //  webClient to complete the subscribe operation
        assert (circuitBreakerEventsBefore.getCircuitBreakerEvents().size() < circuitBreakerEventsAfter.getCircuitBreakerEvents().size());
        assert (events.size() == 2);
    }

    @Test
    public void streamEventsbyNameAndType() throws IOException, InterruptedException {
        List<ServerSentEvent<String>> events = getServerSentEvents(ACTUATOR_STREAM_CIRCUITBREAKER_EVENTS+ "/backendA/ERROR");
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsBefore = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        // The invocation is recorded by the CircuitBreaker as a success.
        dummyService.doSomething(false);
        Thread.sleep(1000); // to record the event
        try {
            dummyService.doSomething(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the CircuitBreaker as part of the recordFailurePredicate as a failure.
        }
        CircuitBreakerEventsEndpointResponse circuitBreakerEventsAfter = circuitBreakerEvents(ACTUATOR_CIRCUITBREAKEREVENTS + "/backendA");
        Thread.sleep(1000); // for webClient to complete the subscribe operation
        assert (circuitBreakerEventsBefore.getCircuitBreakerEvents().size() < circuitBreakerEventsAfter.getCircuitBreakerEvents().size());
        assert (events.size() == 1);
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
        WebClient client = WebClient.create("http://localhost:" + randomServerPort);
        ParameterizedTypeReference<ServerSentEvent<String>> type
            = new ParameterizedTypeReference<ServerSentEvent<String>>() {
        };

        Flux<ServerSentEvent<String>> eventStream = client.get()
            .uri(s)
            .accept(MediaType.TEXT_EVENT_STREAM)
            .retrieve()
            .bodyToFlux(type)
            .take(2);
        return eventStream;
    }
}