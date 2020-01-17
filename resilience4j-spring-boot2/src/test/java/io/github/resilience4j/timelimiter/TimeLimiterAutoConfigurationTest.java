package io.github.resilience4j.timelimiter;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse;
import io.github.resilience4j.service.test.DummyFeignClient;
import io.github.resilience4j.service.test.DummyService;
import io.github.resilience4j.service.test.ReactiveDummyService;
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

import java.io.IOException;
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
    private DummyService dummyService;

    @Autowired
    private ReactiveDummyService reactiveDummyService;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DummyFeignClient dummyFeignClient;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090);

    @Test
    public void testTimeLimiterAutoConfigurationTest() throws Exception {
        assertThat(timeLimiterRegistry).isNotNull();
        assertThat(timeLimiterProperties).isNotNull();

        TimeLimiterEventsEndpointResponse timeLimiterEventsBefore = timeLimiterEvents("/actuator/timelimiterevents");
        TimeLimiterEventsEndpointResponse timeLimiterEventsForABefore = timeLimiterEvents("/actuator/timelimiterevents/backendA");

        try {
            dummyService.doSomethingAsync(true);
        } catch (IOException ex) {
            // Do nothing. The IOException is recorded by the TimeLimiter as a failure.
        }

        final CompletableFuture<String> stringCompletionStage = dummyService.doSomethingAsync(false);
        assertThat(stringCompletionStage.get()).isEqualTo("Test result");

    }

    private TimeLimiterEventsEndpointResponse timeLimiterEvents(String s) {
        return restTemplate.getForEntity(s, TimeLimiterEventsEndpointResponse.class).getBody();
    }

}
