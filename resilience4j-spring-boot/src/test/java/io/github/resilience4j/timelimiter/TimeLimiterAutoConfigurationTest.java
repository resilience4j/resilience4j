package io.github.resilience4j.timelimiter;

import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEndpointResponse;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventDTO;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse;
import io.github.resilience4j.service.test.TestApplication;
import io.github.resilience4j.service.test.TimeLimiterDummyService;
import io.github.resilience4j.timelimiter.autoconfigure.TimeLimiterProperties;
import io.github.resilience4j.timelimiter.configure.TimeLimiterAspect;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import io.prometheus.client.CollectorRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import static io.github.resilience4j.service.test.TimeLimiterDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class)
public class TimeLimiterAutoConfigurationTest {

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    @Autowired
    private TimeLimiterProperties timeLimiterProperties;

    @Autowired
    private TimeLimiterAspect timeLimiterAspect;

    @Autowired
    private TimeLimiterDummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeClass
    public static void setUp() {
        // Need to clear this static registry out since multiple tests register collectors that could collide.
        CollectorRegistry.defaultRegistry.clear();
    }

    /**
     * The test verifies that a TimeLimiter instance is created and configured properly when the DummyService is invoked and
     * that the TimeLimiter records successful and failed calls.
     */
    @Test
    public void testTimeLimiterAutoConfiguration() throws Exception {
        assertThat(timeLimiterRegistry).isNotNull();
        assertThat(timeLimiterProperties).isNotNull();

        try {
            dummyService.doSomething(true).toCompletableFuture().get();
        } catch (Exception ex) {
            // Do nothing. The Exception is recorded by the timelimiter TimeoutException
        }
        // The invocation is recorded by TimeLimiter as a success.
        dummyService.doSomething(false).toCompletableFuture().get();

        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(BACKEND);
        assertThat(timeLimiter).isNotNull();

        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));
        assertThat(timeLimiter.getName()).isEqualTo(BACKEND);

        // expect TimeLimiter actuator endpoint contains both timeLimiters
        ResponseEntity<TimeLimiterEndpointResponse> timeLimiterList = restTemplate.getForEntity("/timelimiter", TimeLimiterEndpointResponse.class);
        assertThat(timeLimiterList.getBody().getTimeLimiters()).hasSize(1).containsOnly(BACKEND);

        // expect TimeLimiter-event actuator endpoint recorded both events
        ResponseEntity<TimeLimiterEventsEndpointResponse> timeLimiterEventList = restTemplate.getForEntity("/timelimiter/events/" + BACKEND, TimeLimiterEventsEndpointResponse.class);

        List<TimeLimiterEventDTO> timeLimiterEvents = timeLimiterEventList.getBody().getTimeLimiterEvents();

        assertThat(timeLimiterEvents.stream()
            .filter(event -> event.getType() == TimeLimiterEvent.Type.SUCCESS)
            .collect(Collectors.toList())).hasSize(1);

        assertThat(timeLimiterEvents.stream()
            .filter(event -> event.getType() == TimeLimiterEvent.Type.TIMEOUT)
            .collect(Collectors.toList())).hasSize(1);

        // expect aspect configured as defined in application.yml
        assertThat(timeLimiterAspect.getOrder()).isEqualTo(500);

    }
}
