package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.spring6.BulkheadDummyService;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.spring6.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.bulkhead.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
public class ThreadPoolBulkHeadInitializationInAspectTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry() {

            ThreadPoolBulkheadConfig backendBulkHeadConfig = ThreadPoolBulkheadConfig.custom()
                    .coreThreadPoolSize(1)
                    .maxThreadPoolSize(1)
                    .build();

            return ThreadPoolBulkheadRegistry.custom()
                    .withThreadPoolBulkheadConfig(ThreadPoolBulkheadConfig.ofDefaults())
                    .addThreadPoolBulkheadConfig(BACKEND, backendBulkHeadConfig)
                    .build();
        }
    }

    @Autowired
    BulkheadDummyService testDummyService;

    @Autowired
    @Qualifier("threadPoolBulkheadRegistry")
    ThreadPoolBulkheadRegistry registry;

    @Before
    public void setUp() {
        // ensure no bulkheads are initialized
        assertThat(registry.getAllBulkheads()).isEmpty();
    }

    @After
    public void tearDown() {
        registry.getAllBulkheads().stream().map(ThreadPoolBulkhead::getName).forEach(registry::remove);
    }

    @Test
    public void testSpelWithoutMappingConfigurationInAspect() throws Exception {
        assertThat(testDummyService.spelSyncThreadPoolNoCfg("foo").toCompletableFuture().get(5, TimeUnit.SECONDS)).isEqualTo("foo");
        assertThat(registry.getAllBulkheads()).hasSize(1).first()
                .matches(bulkhead -> bulkhead.getName().equals("foo"))
                .matches(bulkhead -> bulkhead.getBulkheadConfig() == registry.getDefaultConfig());
    }

    @Test
    public void testSpelWithMappingConfigurationInAspect() throws Exception {
        // The bulkhead is configured to allow 0 concurrent calls, so the call should be rejected
        assertThat(testDummyService.spelSyncThreadPoolWithCfg("foo").toCompletableFuture().get(5, TimeUnit.SECONDS)).isEqualTo("foo");
        assertThat(registry.getAllBulkheads()).hasSize(1).first()
                .matches(bulkhead -> bulkhead.getName().equals("foo"))
                .matches(bulkhead -> bulkhead.getBulkheadConfig() == registry.getConfiguration(BACKEND).orElse(null));
    }
}
