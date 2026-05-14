package io.github.resilience4j.spring6.ratelimiter.configure;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static io.github.resilience4j.spring6.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.ratelimiter.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
class RateLimiterInitializationInAspectTest {

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

    @BeforeEach
    void setUp() {
        // ensure no rate limiters are initialized
        assertThat(registry.getAllRateLimiters()).isEmpty();
    }

    @AfterEach
    void tearDown() {
        registry.getAllRateLimiters().stream().map(RateLimiter::getName).forEach(registry::remove);
    }

    @Test
    void testCorrectConfigIsUsedInAspect() {

        // one successful call within 10s
        assertThat(testDummyService.syncSuccess()).isEqualTo("ok");
        assertThat(testDummyService.syncSuccess()).isEqualTo("recovered");
    }

    @Test
    void testDefaultConfigurationIsUsedIfNoConfigurationAspect() {
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(registry.getAllRateLimiters()).hasSize(1)
                .allMatch(limiter -> limiter.getName().equals("foo"))
                .allMatch(limiter -> limiter.getRateLimiterConfig() == registry.getDefaultConfig());
    }

    @Test
    void testSpecifiedConfigurationIsUsedIfConfigurationAspect() {
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("foo");
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("recovered");
        assertThat(registry.getAllRateLimiters()).hasSize(1)
                .allMatch(limiter -> limiter.getName().equals("foo"))
                .allMatch(limiter -> limiter.getRateLimiterConfig() == registry.getConfiguration(BACKEND).orElse(null));
    }
}
