package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
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
        "logging.level.io.github.resilience4j.bulkhead.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
public class BulkHeadInitializationInAspectTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public BulkheadRegistry bulkheadRegistry() {

            BulkheadConfig backendBulkHeadConfig = BulkheadConfig.custom()
                    .maxConcurrentCalls(0)
                    .build();

            return BulkheadRegistry.custom()
                    .withBulkheadConfig(BulkheadConfig.ofDefaults())
                    .addBulkheadConfig(BACKEND, backendBulkHeadConfig)
                    .build();
        }
    }

    @Autowired
    @Qualifier("bulkheadDummyService")
    TestDummyService testDummyService;

    @Autowired
    BulkheadRegistry registry;

    @Before
    public void setUp() {
        // ensure no bulkheads are initialized
        assertThat(registry.getAllBulkheads()).isEmpty();
    }

    @After
    public void tearDown() {
        // could have used DirtiesContext but spawns a springboot for each test
        registry.getAllBulkheads().stream().map(Bulkhead::getName).forEach(registry::remove);
    }

    @Test
    public void testCorrectConfigIsUsedInAspect() {
        // The bulkhead is configured to allow 0 concurrent calls, so the call should be rejected
        assertThat(testDummyService.syncSuccess()).isEqualTo("recovered");
    }

    @Test
    public void testSpelWithoutMappingConfigurationInAspect() {
        // Default bulkhead is configured to allow several concurrent calls
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(registry.getAllBulkheads()).hasSize(1).first()
                .matches(bulkhead -> bulkhead.getName().equals("foo"))
                .matches(bulkhead -> bulkhead.getBulkheadConfig() == registry.getDefaultConfig());
    }

    @Test
    public void testSpelWithMappingConfigurationInAspect() {
        // The bulkhead is configured to allow 0 concurrent calls, so the call should be rejected
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("recovered");
        assertThat(registry.getAllBulkheads()).hasSize(1).first()
                .matches(bulkhead -> bulkhead.getName().equals("foo"))
                .matches(bulkhead -> bulkhead.getBulkheadConfig() != registry.getDefaultConfig());
    }

    @Test
    public void testConfigurationKeyIsUsedInAspect() {
        assertThat(testDummyService.spelSyncWithCfg(BACKEND)).isEqualTo("recovered");
        assertThat(testDummyService.spelSyncNoCfg(BACKEND)).isEqualTo("recovered");
    }

}
