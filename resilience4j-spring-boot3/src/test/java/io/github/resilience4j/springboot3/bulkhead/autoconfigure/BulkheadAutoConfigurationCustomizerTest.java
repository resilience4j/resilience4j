package io.github.resilience4j.springboot3.bulkhead.autoconfigure;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests combinations of config properties ({@code resilience4j.bulkhead.configs.<name>.*}),
 * instance properties ({@code resilience4j.bulkhead.instances.<name>.*}) and {@link BulkheadConfigCustomizer}.
 * <p>
 * To make this test easier to follow it always uses different magnitude of values for different ways to configure a bulkhead:
 * <ul>
 *     <li>config properties - N * 10</li>
 *     <li>instance properties - N * 100</li>
 *     <li>customizer - N * 1000</li>
 * </ul>
 * where N is index of the config. This way when asserting value {@code 200} it is guaranteed to be coming from instance properties.
 */
public class BulkheadAutoConfigurationCustomizerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BulkheadAutoConfiguration.class))
        .withPropertyValues(
            "resilience4j.bulkhead.configs.default.writableStackTraceEnabled: true",
            "resilience4j.bulkhead.configs.default.maxConcurrentCalls: 10",
            "resilience4j.bulkhead.configs.default.maxWaitDuration: 10ms",
            "resilience4j.bulkhead.configs.sharedConfig.writableStackTraceEnabled: false",
            "resilience4j.bulkhead.configs.sharedConfig.maxConcurrentCalls: 20",
            "resilience4j.bulkhead.configs.sharedConfig.maxWaitDuration: 20ms"
        );

    @Test
    public void testUserConfigShouldBeAbleToProvideCustomizers() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.bulkhead.instances.backendWithoutSharedConfig.writableStackTraceEnabled: false",
                "resilience4j.bulkhead.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.bulkhead.instances.backendWithSharedConfig.writableStackTraceEnabled: true"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(BulkheadRegistry.class);
                BulkheadRegistry registry = context.getBean(BulkheadRegistry.class);

                BulkheadConfig backendWithoutSharedConfig = registry.bulkhead("backendWithoutSharedConfig").getBulkheadConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(10));
                // from instance config
                assertThat(backendWithoutSharedConfig.isWritableStackTraceEnabled()).isEqualTo(false);
                // from customizer
                assertThat(backendWithoutSharedConfig.getMaxConcurrentCalls()).isEqualTo(1000);

                BulkheadConfig backendWithSharedConfig = registry.bulkhead("backendWithSharedConfig").getBulkheadConfig();
                // from default config
                assertThat(backendWithSharedConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(20));
                // from instance config
                assertThat(backendWithSharedConfig.isWritableStackTraceEnabled()).isEqualTo(true);
                // from customizer
                assertThat(backendWithSharedConfig.getMaxConcurrentCalls()).isEqualTo(2000);

                BulkheadConfig backendWithoutInstanceConfig = registry.bulkhead("backendWithoutInstanceConfig").getBulkheadConfig();
                // from default config
                assertThat(backendWithoutInstanceConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(10));
                // from default config
                assertThat(backendWithoutInstanceConfig.isWritableStackTraceEnabled()).isEqualTo(true);
                // from customizer
                assertThat(backendWithoutInstanceConfig.getMaxConcurrentCalls()).isEqualTo(3000);
            });
    }

    @Test
    public void testCustomizersShouldOverrideProperties() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.bulkhead.instances.backendWithoutSharedConfig.maxConcurrentCalls: 100"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(BulkheadRegistry.class);
                BulkheadRegistry registry = context.getBean(BulkheadRegistry.class);

                BulkheadConfig backendWithoutSharedConfig = registry.bulkhead("backendWithoutSharedConfig").getBulkheadConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(10));
                // from default config
                assertThat(backendWithoutSharedConfig.isWritableStackTraceEnabled()).isEqualTo(true);
                // from customizer
                assertThat(backendWithoutSharedConfig.getMaxConcurrentCalls()).isEqualTo(1000);
            });
    }

    @Test
    public void testCustomizersAreAppliedOnConfigs() {
        // Given
        contextRunner.withUserConfiguration(ConfigCustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.bulkhead.instances.backendWithoutSharedConfig.writableStackTraceEnabled: false",
                "resilience4j.bulkhead.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.bulkhead.instances.backendWithSharedConfig.writableStackTraceEnabled: true"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(BulkheadRegistry.class);
                BulkheadRegistry registry = context.getBean(BulkheadRegistry.class);

                BulkheadConfig defaultConfig = registry.getConfiguration("default").orElseThrow();
                // from customizer
                assertThat(defaultConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(1000));
                // from customizer
                assertThat(defaultConfig.isWritableStackTraceEnabled()).isEqualTo(true);
                // from customizer
                assertThat(defaultConfig.getMaxConcurrentCalls()).isEqualTo(1000);

                BulkheadConfig backendWithoutSharedConfig = registry.bulkhead("backendWithoutSharedConfig").getBulkheadConfig();
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(1000));
                // from instance config
                assertThat(backendWithoutSharedConfig.isWritableStackTraceEnabled()).isEqualTo(false);
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getMaxConcurrentCalls()).isEqualTo(1000);


                BulkheadConfig backendWithSharedConfig = registry.bulkhead("backendWithSharedConfig").getBulkheadConfig();
                // from shared config customizer
                assertThat(backendWithSharedConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(2000));
                // from instance config
                assertThat(backendWithSharedConfig.isWritableStackTraceEnabled()).isEqualTo(true);
                // from shared config customizer
                assertThat(backendWithSharedConfig.getMaxConcurrentCalls()).isEqualTo(2000);

                BulkheadConfig backendWithoutInstanceConfig = registry.bulkhead("backendWithoutInstanceConfig").getBulkheadConfig();
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(1000));
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.isWritableStackTraceEnabled()).isEqualTo(true);
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getMaxConcurrentCalls()).isEqualTo(1000);
            });
    }

    @Configuration
    public static class CustomizerConfiguration {
        @Bean
        public BulkheadConfigCustomizer backendWithoutSharedConfigCustomizer() {
            return BulkheadConfigCustomizer.of("backendWithoutSharedConfig",
                builder -> builder.maxConcurrentCalls(1000)
            );
        }

        @Bean
        public BulkheadConfigCustomizer backendWithSharedConfigCustomizer() {
            return BulkheadConfigCustomizer.of("backendWithSharedConfig",
                builder -> builder.maxConcurrentCalls(2000)
            );
        }

        @Bean
        public BulkheadConfigCustomizer backendWithoutInstanceConfigCustomizer() {
            return BulkheadConfigCustomizer.of("backendWithoutInstanceConfig",
                builder -> builder.maxConcurrentCalls(3000)
            );
        }
    }

    @Configuration
    public static class ConfigCustomizerConfiguration {
        @Bean
        public BulkheadConfigCustomizer defaultCustomizer() {
            return BulkheadConfigCustomizer.of("default",
                builder -> builder.writableStackTraceEnabled(true)
                    .maxConcurrentCalls(1000)
                    .maxWaitDuration(Duration.ofMillis(1000))
            );
        }

        @Bean
        public BulkheadConfigCustomizer sharedConfigCustomizer() {
            return BulkheadConfigCustomizer.of("sharedConfig",
                builder -> builder.writableStackTraceEnabled(false)
                    .maxConcurrentCalls(2000)
                    .maxWaitDuration(Duration.ofMillis(2000))
            );
        }
    }
}
