package io.github.resilience4j.spring6.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;

import static io.github.resilience4j.spring6.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.ratelimiter.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
public class RateLimiterInitializationInAspectTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public RateLimiterRegistry rateLimiterRegistry() {

            RateLimiterConfig backendRateLimiterConfig = RateLimiterConfig.custom()
                    .limitForPeriod(1)
                    .limitRefreshPeriod(Duration.ofSeconds(10))
                    .timeoutDuration(Duration.ofMillis(1))
                    .build();

            return RateLimiterRegistry.custom()
                    .withRateLimiterConfig(RateLimiterConfig.ofDefaults())
                    .addRateLimiterConfig(BACKEND, backendRateLimiterConfig)
                    .build();
        }
    }

    @Autowired
    @Qualifier("rateLimiterDummyService")
    TestDummyService testDummyService;

    @Autowired
    RateLimiterRegistry registry;

    @Before
    public void setUp() {
        // ensure no rate limiters are initialized
        assertThat(registry.getAllRateLimiters()).isEmpty();
    }

    @After
    public void tearDown() {
        registry.getAllRateLimiters().stream().map(RateLimiter::getName).forEach(registry::remove);
    }

    @Test
    public void testCorrectConfigIsUsedInAspect() {

        // one successful call within 10s
        assertThat(testDummyService.syncSuccess()).isEqualTo("ok");
        assertThat(testDummyService.syncSuccess()).isEqualTo("recovered");
    }

    @Test
    public void testDefaultConfigurationIsUsedIfNoConfigurationAspect() {
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(registry.getAllRateLimiters()).hasSize(1)
                .allMatch(limiter -> limiter.getName().equals("foo"))
                .allMatch(limiter -> limiter.getRateLimiterConfig() == registry.getDefaultConfig());
    }

    @Test
    public void testSpecifiedConfigurationIsUsedIfConfigurationAspect() {
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("foo");
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("recovered");
        assertThat(registry.getAllRateLimiters()).hasSize(1)
                .allMatch(limiter -> limiter.getName().equals("foo"))
                .allMatch(limiter -> limiter.getRateLimiterConfig() == registry.getConfiguration(BACKEND).orElse(null));
    }
}
