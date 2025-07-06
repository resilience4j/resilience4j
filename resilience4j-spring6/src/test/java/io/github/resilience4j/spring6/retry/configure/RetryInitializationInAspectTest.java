package io.github.resilience4j.spring6.retry.configure;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
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

import static io.github.resilience4j.spring6.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.retry.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
public class RetryInitializationInAspectTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public RetryRegistry retryRegistry() {

            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(4)     // more than the default
                    .failAfterMaxAttempts(true)
                    .build();

            return RetryRegistry.custom()
                    .withRetryConfig(RetryConfig.ofDefaults())
                    .addRetryConfig(BACKEND, retryConfig)
                    .build();
        }
    }

    @Autowired
    @Qualifier("retryDummyService")
    TestDummyService testDummyService;

    @Autowired
    RetryRegistry registry;

    @Before
    public void setUp() {
        // ensure no retries are initialized
        assertThat(registry.getAllRetries()).isEmpty();
    }

    @After
    public void tearDown() throws Exception {
        registry.getAllRetries().stream().map(Retry::getName).forEach(registry::remove);
    }

    @Test
    public void testCorrectConfigIsUsedInAspect() {

        assertThat(testDummyService.syncSuccess()).isEqualTo("ok");
    }

    @Test
    public void testSpelWithoutConfiguration() {
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");

        assertThat(registry.getAllRetries()).hasSize(1).first()
                .matches(retry ->  retry.getName().equals("foo"))
                .matches(timeLimiter -> timeLimiter.getRetryConfig() == registry.getDefaultConfig());
    }

    @Test
    public void testSpelWithConfiguration() {
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("foo");

        assertThat(registry.getAllRetries()).hasSize(1).first()
                .matches(retry ->  retry.getName().equals("foo"))
                .matches(timeLimiter -> timeLimiter.getRetryConfig() == registry.getConfiguration(BACKEND).orElse(null));
    }
}
