package io.github.resilience4j.timelimiter;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.TestApplication;
import io.github.resilience4j.timelimiter.autoconfigure.TimeLimiterProperties;
import io.github.resilience4j.timelimiter.configure.TimeLimiterAspect;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class)
public class TimeLimiterAutoConfigurationTest {

    @Autowired
    TimeLimiterRegistry timeLimiterRegistry;

    @Autowired
    TimeLimiterProperties timeLimiterProperties;

    @Autowired
    TimeLimiterAspect timeLimiterAspect;

    @Autowired
    CompositeCustomizer<TimeLimiterConfigCustomizer> compositeTimeLimiterConfigCustomizer;

    @Autowired
    private DummyService dummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);

    @Test
    public void testTimeLimiterAutoConfigurationTest() throws Exception {
        assertThat(timeLimiterRegistry).isNotNull();
        assertThat(timeLimiterProperties).isNotNull();
        assertThat(compositeTimeLimiterConfigCustomizer).isNotNull();
        assertThat(compositeTimeLimiterConfigCustomizer.getCustomizer("timeLimiterBackendD").isPresent()).isTrue();
        assertThat(timeLimiterRegistry.getTags()).isNotEmpty();

        TimeLimiterEventsEndpointResponse timeLimiterEventsBefore =
            timeLimiterEvents("/actuator/timelimiterevents");
        TimeLimiterEventsEndpointResponse timeLimiterEventsForABefore =
            timeLimiterEvents("/actuator/timelimiterevents/backendA");

        try {
            dummyService.doSomethingAsync(true).get();
        } catch (Exception ex) {
            // Do nothing. The IOException is recorded by the TimeLimiter as a failure.
        }

        final CompletableFuture<String> stringCompletionStage = dummyService.doSomethingAsync(false);
        assertThat(stringCompletionStage.get()).isEqualTo("Test result");

        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(DummyService.BACKEND);
        assertThat(timeLimiter).isNotNull();

        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));

        TimeLimiterEventsEndpointResponse timeLimiterEventList = timeLimiterEvents("/actuator/timelimiterevents");
        assertThat(timeLimiterEventList.getTimeLimiterEvents())
            .hasSize(timeLimiterEventsBefore.getTimeLimiterEvents().size() + 2);

        timeLimiterEventList = timeLimiterEvents("/actuator/timelimiterevents/backendA");
        assertThat(timeLimiterEventList.getTimeLimiterEvents())
            .hasSize(timeLimiterEventsForABefore.getTimeLimiterEvents().size() + 2);

        assertThat(timeLimiterAspect.getOrder()).isEqualTo(398);
    }

    private TimeLimiterEventsEndpointResponse timeLimiterEvents(String s) {
        return restTemplate.getForEntity(s, TimeLimiterEventsEndpointResponse.class).getBody();
    }

}
