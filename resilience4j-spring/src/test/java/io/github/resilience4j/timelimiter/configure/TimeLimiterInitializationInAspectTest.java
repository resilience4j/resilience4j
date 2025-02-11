package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.TimeLimiterDummyService;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.Map;

import static io.github.resilience4j.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.timelimiter.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
public class TimeLimiterInitializationInAspectTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public TimeLimiterRegistry timeLimiterRegistry() {

            TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                    .timeoutDuration(Duration.ofSeconds(3))
                    .build();

            return TimeLimiterRegistry.of(
                    Map.of(BACKEND, timeLimiterConfig)
            );
        }
    }

    @Autowired
    TimeLimiterDummyService testDummyService;

    @Autowired
    TimeLimiterRegistry registry;

    @Before
    public void setUp() {
        // ensure no time limiters are initialized
        assertThat(registry.getAllTimeLimiters()).isEmpty();
    }

    @Test
    public void testCorrectConfigIsUsedInAspect() throws Exception {

        // Should not time out because the time limit is 3 seconds
        assertThat(testDummyService.success().toCompletableFuture().get())
                .isEqualTo("ok");
    }
}
