package io.github.resilience4j.springboot.timelimiter;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.common.timelimiter.monitoring.endpoint.TimeLimiterEventsEndpointResponse;
import io.github.resilience4j.springboot.service.test.DummyService;
import io.github.resilience4j.springboot.service.test.TestApplication;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;
import io.github.resilience4j.springboot.timelimiter.autoconfigure.TimeLimiterProperties;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterAspect;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = TestApplication.class)
@AutoConfigureTestRestTemplate
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

    @Test
    public void shouldThrowTimeOutExceptionAndPropagateContext() throws InterruptedException {
        TimeLimiterEventsEndpointResponse timeLimiterEventsBefore =
            timeLimiterEvents("/actuator/timelimiterevents");
        TimeLimiterEventsEndpointResponse timeLimiterEventsForABefore =
            timeLimiterEvents("/actuator/timelimiterevents/backendB");

        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(DummyService.BACKEND_B);
        assertThat(timeLimiter).isNotNull();

        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        final CompletableFuture<String> future = dummyService.longDoSomethingAsync().exceptionally(throwable -> {
            if (throwable != null) {
                assertThat(Thread.currentThread().getName()).contains("ContextAwareScheduledThreadPool-");
                assertThat(TestThreadLocalContextHolder.get().get()).isEqualTo("ValueShouldCrossThreadBoundary");
                return (String) TestThreadLocalContextHolder.get().orElse(null);
            }
            return null;
        });

        waitAtMost(3, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("ValueShouldCrossThreadBoundary")));

        TimeLimiterEventsEndpointResponse timeLimiterEventList = timeLimiterEvents("/actuator/timelimiterevents");
        assertThat(timeLimiterEventList.getTimeLimiterEvents())
            .hasSize(timeLimiterEventsBefore.getTimeLimiterEvents().size() + 1);

        timeLimiterEventList = timeLimiterEvents("/actuator/timelimiterevents/backendB");
        assertThat(timeLimiterEventList.getTimeLimiterEvents())
            .hasSize(timeLimiterEventsForABefore.getTimeLimiterEvents().size() + 1);

        assertThat(timeLimiterAspect.getOrder()).isEqualTo(398);
    }

    @Test
    public void shouldThrowTimeOutExceptionAndPropagateMDCContext() throws InterruptedException {
        TimeLimiterEventsEndpointResponse timeLimiterEventsBefore =
            timeLimiterEvents("/actuator/timelimiterevents");
        TimeLimiterEventsEndpointResponse timeLimiterEventsForABefore =
            timeLimiterEvents("/actuator/timelimiterevents/backendB");

        TimeLimiter timeLimiter = timeLimiterRegistry.timeLimiter(DummyService.BACKEND_B);
        assertThat(timeLimiter).isNotNull();

        assertThat(timeLimiter.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(1));

        MDC.put("key", "ValueShouldCrossThreadBoundary");
        MDC.put("key2","value2");
        final Map<String, String> contextMap = MDC.getCopyOfContextMap();
        final CompletableFuture<String> future = dummyService.longDoSomethingAsync().exceptionally(throwable -> {
            if (throwable != null) {
                assertThat(Thread.currentThread().getName()).contains("ContextAwareScheduledThreadPool-");
                assertThat(MDC.getCopyOfContextMap()).hasSize(2).containsExactlyEntriesOf(contextMap);
                return MDC.getCopyOfContextMap().get("key");
            }
            return null;
        });

        waitAtMost(3, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("ValueShouldCrossThreadBoundary")));

        TimeLimiterEventsEndpointResponse timeLimiterEventList = timeLimiterEvents("/actuator/timelimiterevents");
        assertThat(timeLimiterEventList.getTimeLimiterEvents())
            .hasSize(timeLimiterEventsBefore.getTimeLimiterEvents().size() + 1);

        timeLimiterEventList = timeLimiterEvents("/actuator/timelimiterevents/backendB");
        assertThat(timeLimiterEventList.getTimeLimiterEvents())
            .hasSize(timeLimiterEventsForABefore.getTimeLimiterEvents().size() + 1);

        assertThat(timeLimiterAspect.getOrder()).isEqualTo(398);
    }

    private TimeLimiterEventsEndpointResponse timeLimiterEvents(String s) {
        return restTemplate.getForEntity(s, TimeLimiterEventsEndpointResponse.class).getBody();
    }

}
