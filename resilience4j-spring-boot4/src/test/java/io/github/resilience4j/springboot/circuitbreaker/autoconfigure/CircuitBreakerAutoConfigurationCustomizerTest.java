package io.github.resilience4j.springboot.circuitbreaker.autoconfigure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests combinations of config properties ({@code resilience4j.circuitbreaker.configs.<name>.*}),
 * instance properties ({@code resilience4j.circuitbreaker.instances.<name>.*}) and {@link CircuitBreakerConfigCustomizer}.
 * <p>
 * To make this test easier to follow it always uses different magnitude of values for different ways to configure a circuit breaker:
 * <ul>
 *     <li>config properties - N * 10</li>
 *     <li>instance properties - N * 100</li>
 *     <li>customizer - N * 1000</li>
 * </ul>
 * where N is index of the config. This way when asserting value {@code 200} it is guaranteed to be coming from instance properties.
 */
public class CircuitBreakerAutoConfigurationCustomizerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CircuitBreakerAutoConfiguration.class))
        .withPropertyValues(
            "resilience4j.circuitbreaker.configs.default.slidingWindowSize: 10",
            "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState: 10",
            "resilience4j.circuitbreaker.configs.default.slowCallDurationThreshold: 10ms",
            "resilience4j.circuitbreaker.configs.sharedConfig.slidingWindowSize: 20",
            "resilience4j.circuitbreaker.configs.sharedConfig.permittedNumberOfCallsInHalfOpenState: 20",
            "resilience4j.circuitbreaker.configs.sharedConfig.slowCallDurationThreshold: 20ms"
        );

    @Test
    public void testUserConfigShouldBeAbleToProvideCustomizers() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.circuitbreaker.instances.backendWithoutSharedConfig.slidingWindowSize: 100",
                "resilience4j.circuitbreaker.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.circuitbreaker.instances.backendWithSharedConfig.slidingWindowSize: 200"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(CircuitBreakerRegistry.class);
                CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);

                CircuitBreakerConfig backendWithoutSharedConfig = registry.circuitBreaker("backendWithoutSharedConfig").getCircuitBreakerConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(10));
                // from instance config
                assertThat(backendWithoutSharedConfig.getSlidingWindowSize()).isEqualTo(100);
                // from customizer
                assertThat(backendWithoutSharedConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1000);

                CircuitBreakerConfig backendWithSharedConfig = registry.circuitBreaker("backendWithSharedConfig").getCircuitBreakerConfig();
                // from default config
                assertThat(backendWithSharedConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(20));
                // from instance config
                assertThat(backendWithSharedConfig.getSlidingWindowSize()).isEqualTo(200);
                // from customizer
                assertThat(backendWithSharedConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2000);

                CircuitBreakerConfig backendWithoutInstanceConfig = registry.circuitBreaker("backendWithoutInstanceConfig").getCircuitBreakerConfig();
                // from default config
                assertThat(backendWithoutInstanceConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(10));
                // from default config
                assertThat(backendWithoutInstanceConfig.getSlidingWindowSize()).isEqualTo(10);
                // from customizer
                assertThat(backendWithoutInstanceConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3000);
            });
    }

    @Test
    public void testCustomizersShouldOverrideProperties() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.circuitbreaker.instances.backendWithoutSharedConfig.permittedNumberOfCallsInHalfOpenState: 100"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(CircuitBreakerRegistry.class);
                CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);

                CircuitBreakerConfig backendWithoutSharedConfig = registry.circuitBreaker("backendWithoutSharedConfig").getCircuitBreakerConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(10));
                // from default config
                assertThat(backendWithoutSharedConfig.getSlidingWindowSize()).isEqualTo(10);
                // from customizer
                assertThat(backendWithoutSharedConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1000);
            });
    }

    @Test
    public void testCustomizersAreAppliedOnConfigs() {
        // Given
        contextRunner.withUserConfiguration(ConfigCustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.circuitbreaker.instances.backendWithoutSharedConfig.slidingWindowSize: 100",
                "resilience4j.circuitbreaker.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.circuitbreaker.instances.backendWithSharedConfig.slidingWindowSize: 200"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(CircuitBreakerRegistry.class);
                CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);

                CircuitBreakerConfig defaultConfig = registry.getConfiguration("default").orElseThrow();
                // from customizer
                assertThat(defaultConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(1000));
                // from customizer
                assertThat(defaultConfig.getSlidingWindowSize()).isEqualTo(1000);
                // from customizer
                assertThat(defaultConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1000);

                CircuitBreakerConfig backendWithoutSharedConfig = registry.circuitBreaker("backendWithoutSharedConfig").getCircuitBreakerConfig();
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(1000));
                // from instance config
                assertThat(backendWithoutSharedConfig.getSlidingWindowSize()).isEqualTo(100);
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1000);


                CircuitBreakerConfig backendWithSharedConfig = registry.circuitBreaker("backendWithSharedConfig").getCircuitBreakerConfig();
                // from shared config customizer
                assertThat(backendWithSharedConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(2000));
                // from instance config
                assertThat(backendWithSharedConfig.getSlidingWindowSize()).isEqualTo(200);
                // from shared config customizer
                assertThat(backendWithSharedConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2000);

                CircuitBreakerConfig backendWithoutInstanceConfig = registry.circuitBreaker("backendWithoutInstanceConfig").getCircuitBreakerConfig();
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getSlowCallDurationThreshold()).isEqualTo(Duration.ofMillis(1000));
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getSlidingWindowSize()).isEqualTo(1000);
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(1000);
            });
    }

    @Configuration
    public static class CustomizerConfiguration {
        @Bean
        public CircuitBreakerConfigCustomizer backendWithoutSharedConfigCustomizer() {
            return CircuitBreakerConfigCustomizer.of("backendWithoutSharedConfig",
                builder -> builder.permittedNumberOfCallsInHalfOpenState(1000)
            );
        }

        @Bean
        public CircuitBreakerConfigCustomizer backendWithSharedConfigCustomizer() {
            return CircuitBreakerConfigCustomizer.of("backendWithSharedConfig",
                builder -> builder.permittedNumberOfCallsInHalfOpenState(2000)
            );
        }

        @Bean
        public CircuitBreakerConfigCustomizer backendWithoutInstanceConfigCustomizer() {
            return CircuitBreakerConfigCustomizer.of("backendWithoutInstanceConfig",
                builder -> builder.permittedNumberOfCallsInHalfOpenState(3000)
            );
        }
    }

    @Configuration
    public static class ConfigCustomizerConfiguration {
        @Bean
        public CircuitBreakerConfigCustomizer defaultCustomizer() {
            return CircuitBreakerConfigCustomizer.of("default",
                builder -> builder.slidingWindowSize(1000)
                    .permittedNumberOfCallsInHalfOpenState(1000)
                    .slowCallDurationThreshold(Duration.ofMillis(1000))
            );
        }

        @Bean
        public CircuitBreakerConfigCustomizer sharedConfigCustomizer() {
            return CircuitBreakerConfigCustomizer.of("sharedConfig",
                builder -> builder.slidingWindowSize(2000)
                    .permittedNumberOfCallsInHalfOpenState(2000)
                    .slowCallDurationThreshold(Duration.ofMillis(2000))
            );
        }
    }
}
