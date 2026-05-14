package io.github.resilience4j.spring6.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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

import static io.github.resilience4j.spring6.TestDummyService.BACKEND;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(properties = {
        "logging.level.io.github.resilience4j.circuitbreaker.configure=debug",
        "spring.main.allow-bean-definition-overriding=true"
})
class CircuitBreakerInitializationInAspectTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public CircuitBreakerRegistry circuitBreakerRegistry() {

            CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                    .initialState(CircuitBreaker.State.OPEN)
                    .build();

            return CircuitBreakerRegistry.custom()
                    .withCircuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                    .addCircuitBreakerConfig(BACKEND, circuitBreakerConfig)
                    .build();
        }
    }

    @Autowired
    @Qualifier("circuitBreakerDummyService")
    TestDummyService testDummyService;

    @Autowired
    CircuitBreakerRegistry registry;

    @BeforeEach
    void setUp() {
        // ensure no circuit breakers are initialized
        assertThat(registry.getAllCircuitBreakers()).isEmpty();
    }

    @AfterEach
    void tearDown() {
        registry.getAllCircuitBreakers().stream().map(CircuitBreaker::getName).forEach(registry::remove);
    }

    @Test
    void testCorrectConfigIsUsedInAspect() {

        // The circuit breaker is configured to start in the OPEN state, so the call should be rejected
        assertThat(testDummyService.syncSuccess()).isEqualTo("recovered");
    }

    @Test
    void testSpelWithoutConfigurationInAspect() {
        // default circuit breaker is configured to start in CLOSE state
        assertThat(testDummyService.spelSyncNoCfg("foo")).isEqualTo("foo");
        assertThat(registry.getAllCircuitBreakers()).hasSize(1).first()
                .matches(circuitBreaker -> circuitBreaker.getName().equals("foo"))
                .matches(circuitBreaker -> circuitBreaker.getCircuitBreakerConfig() == registry.getDefaultConfig());
    }

    @Test
    void testSpelWithConfigurationInAspect() {
        // backend circuit breaker is configured to start in the OPEN state, so the call should be rejected
        assertThat(testDummyService.spelSyncWithCfg("foo")).isEqualTo("recovered");
        assertThat(registry.getAllCircuitBreakers()).hasSize(1).first()
                .matches(circuitBreaker -> circuitBreaker.getName().equals("foo"))
                .matches(circuitBreaker -> circuitBreaker.getCircuitBreakerConfig() == registry.getConfiguration(BACKEND).orElse(null));
    }
}
