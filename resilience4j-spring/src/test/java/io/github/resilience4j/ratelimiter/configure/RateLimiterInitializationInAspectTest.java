package io.github.resilience4j.ratelimiter.configure;

import io.github.resilience4j.TestCustomRegistryApplication;
import io.github.resilience4j.TestDummyService;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestCustomRegistryApplication.class,
        properties = {
                "logging.level.io.github.resilience4j.ratelimiter.configure=debug",
                "spring.main.allow-bean-definition-overriding=true"
        })
public class RateLimiterInitializationInAspectTest {

    @Autowired
    @Qualifier("rateLimiterSuccessDummyService")
    TestDummyService testDummyService;

    @Autowired
    RateLimiterRegistry registry;

    @Before
    public void setUp() {
        // ensure no rate limiters are initialized
        assertThat(registry.getAllRateLimiters()).isEmpty();
    }

    @Test
    public void testCorrectConfigIsUsedInAspect() {

        // one successful call within 10s
        assertThat(testDummyService.sync()).isEqualTo("success");
        assertThat(testDummyService.sync()).isEqualTo("recovered");
    }
}
