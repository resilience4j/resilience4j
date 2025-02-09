package io.github.resilience4j.retry.configure;

import io.github.resilience4j.TestDummyService;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static io.github.resilience4j.TestDummyService.BACKEND;
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
        // ensure no reties are initialized
        assertThat(registry.getAllRetries()).isEmpty();
    }

    @Test
    public void testCorrectConfigIsUsedInAspect() {

        assertThat(testDummyService.syncSuccess()).isEqualTo("ok");
    }
}
