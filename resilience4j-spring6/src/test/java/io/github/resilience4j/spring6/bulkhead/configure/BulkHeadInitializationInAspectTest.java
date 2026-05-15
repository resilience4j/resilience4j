package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.spring6.TestDummyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static io.github.resilience4j.spring6.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.bulkhead.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
class BulkHeadInitializationInAspectTest {

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

    @BeforeEach
    void setUp() {
        // ensure no bulkheads are initialized
        assertThat(registry.getAllBulkheads()).isEmpty();
    }

    @AfterEach
    void tearDown() {
        // could have used DirtiesContext but spawns a springboot for each test
        registry.getAllBulkheads().stream().map(Bulkhead::getName).forEach(registry::remove);
    }

    @Test
    void testCorrectConfigIsUsedInAspect() {
        // The bulkhead is configured to allow 0 concurrent calls, so the call should be rejected
        assertThat(testDummyService.syncSuccess()).isEqualTo("recovered");
    }

    @Test
    void testSpelWithoutMappingConfigurationInAspect() {
        // Default bulkhead is configured to allow several concurrent calls
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(registry.getAllBulkheads()).hasSize(1).first()
                .matches(bulkhead -> bulkhead.getName().equals("foo"))
                .matches(bulkhead -> bulkhead.getBulkheadConfig() == registry.getDefaultConfig());
    }

    @Test
    void testSpelWithMappingConfigurationInAspect() {
        // The bulkhead is configured to allow 0 concurrent calls, so the call should be rejected
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("recovered");
        assertThat(registry.getAllBulkheads()).hasSize(1).first()
                .matches(bulkhead -> bulkhead.getName().equals("foo"))
                .matches(bulkhead -> bulkhead.getBulkheadConfig() == registry.getConfiguration(BACKEND).orElse(null));
    }

    @Test
    void testConfigurationKeyIsUsedInAspect() {
        assertThat(testDummyService.spelSyncWithCfg(BACKEND)).isEqualTo("recovered");
        assertThat(testDummyService.spelSyncNoCfg(BACKEND)).isEqualTo("recovered");
    }
}
