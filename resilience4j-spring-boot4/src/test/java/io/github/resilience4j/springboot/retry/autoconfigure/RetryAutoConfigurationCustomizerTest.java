package io.github.resilience4j.springboot.retry.autoconfigure;

import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests combinations of config properties ({@code resilience4j.retry.configs.<name>.*}),
 * instance properties ({@code resilience4j.retry.instances.<name>.*}) and {@link RetryConfigCustomizer}.
 * <p>
 * To make this test easier to follow it always uses different magnitude of values for different ways to configure a retry:
 * <ul>
 *     <li>config properties - N * 10</li>
 *     <li>instance properties - N * 100</li>
 *     <li>customizer - N * 1000</li>
 * </ul>
 * where N is index of the config. This way when asserting value {@code 200} it is guaranteed to be coming from instance properties.
 */
public class RetryAutoConfigurationCustomizerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class))
        .withPropertyValues(
            "resilience4j.retry.configs.default.maxAttempts: 10",
            "resilience4j.retry.configs.default.writableStackTraceEnabled: true",
            "resilience4j.retry.configs.default.waitDuration: 10ms",
            "resilience4j.retry.configs.sharedConfig.maxAttempts: 20",
            "resilience4j.retry.configs.sharedConfig.writableStackTraceEnabled: false",
            "resilience4j.retry.configs.sharedConfig.waitDuration: 20ms"
        );

    @Test
    public void testUserConfigShouldBeAbleToProvideCustomizers() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.retry.instances.backendWithoutSharedConfig.maxAttempts: 100",
                "resilience4j.retry.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.retry.instances.backendWithSharedConfig.maxAttempts: 200"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);

                RetryConfig backendWithoutSharedConfig = registry.retry("backendWithoutSharedConfig").getRetryConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(10);
                // from instance config
                assertThat(backendWithoutSharedConfig.getMaxAttempts()).isEqualTo(100);
                // from customizer
                assertThat(backendWithoutSharedConfig.isWritableStackTraceEnabled()).isEqualTo(true);

                RetryConfig backendWithSharedConfig = registry.retry("backendWithSharedConfig").getRetryConfig();
                // from default config
                assertThat(backendWithSharedConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(20);
                // from instance config
                assertThat(backendWithSharedConfig.getMaxAttempts()).isEqualTo(200);
                // from customizer
                assertThat(backendWithSharedConfig.isWritableStackTraceEnabled()).isEqualTo(false);

                RetryConfig backendWithoutInstanceConfig = registry.retry("backendWithoutInstanceConfig").getRetryConfig();
                // from default config
                assertThat(backendWithoutInstanceConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(10);
                // from default config
                assertThat(backendWithoutInstanceConfig.getMaxAttempts()).isEqualTo(10);
                // from customizer
                assertThat(backendWithoutInstanceConfig.isWritableStackTraceEnabled()).isEqualTo(true);
            });
    }

    @Test
    public void testCustomizersShouldOverrideProperties() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.retry.instances.backendWithoutSharedConfig.writableStackTraceEnabled: false"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);

                RetryConfig backendWithoutSharedConfig = registry.retry("backendWithoutSharedConfig").getRetryConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(10);
                // from default config
                assertThat(backendWithoutSharedConfig.getMaxAttempts()).isEqualTo(10);
                // from customizer
                assertThat(backendWithoutSharedConfig.isWritableStackTraceEnabled()).isEqualTo(true);
            });
    }

    @Test
    public void testCustomizersAreAppliedOnConfigs() {
        // Given
        contextRunner.withUserConfiguration(ConfigCustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.retry.instances.backendWithoutSharedConfig.maxAttempts: 100",
                "resilience4j.retry.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.retry.instances.backendWithSharedConfig.maxAttempts: 200"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);

                RetryConfig defaultConfig = registry.getConfiguration("default").orElseThrow();
                // from customizer
                assertThat(defaultConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(1000);
                // from customizer
                assertThat(defaultConfig.getMaxAttempts()).isEqualTo(1000);
                // from customizer
                assertThat(defaultConfig.isWritableStackTraceEnabled()).isEqualTo(true);

                RetryConfig backendWithoutSharedConfig = registry.retry("backendWithoutSharedConfig").getRetryConfig();
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(1000);
                // from instance config
                assertThat(backendWithoutSharedConfig.getMaxAttempts()).isEqualTo(100);
                // from default config customizer
                assertThat(backendWithoutSharedConfig.isWritableStackTraceEnabled()).isEqualTo(true);


                RetryConfig backendWithSharedConfig = registry.retry("backendWithSharedConfig").getRetryConfig();
                // from shared config customizer
                assertThat(backendWithSharedConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(2000);
                // from instance config
                assertThat(backendWithSharedConfig.getMaxAttempts()).isEqualTo(200);
                // from shared config customizer
                assertThat(backendWithSharedConfig.isWritableStackTraceEnabled()).isEqualTo(false);

                RetryConfig backendWithoutInstanceConfig = registry.retry("backendWithoutInstanceConfig").getRetryConfig();
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getIntervalBiFunction().apply(0, null)).isEqualTo(1000);
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getMaxAttempts()).isEqualTo(1000);
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.isWritableStackTraceEnabled()).isEqualTo(true);
            });
    }

    @Configuration
    public static class CustomizerConfiguration {
        @Bean
        public RetryConfigCustomizer backendWithoutSharedConfigCustomizer() {
            return RetryConfigCustomizer.of("backendWithoutSharedConfig",
                builder -> builder.writableStackTraceEnabled(true)
            );
        }

        @Bean
        public RetryConfigCustomizer backendWithSharedConfigCustomizer() {
            return RetryConfigCustomizer.of("backendWithSharedConfig",
                builder -> builder.writableStackTraceEnabled(false)
            );
        }

        @Bean
        public RetryConfigCustomizer backendWithoutInstanceConfigCustomizer() {
            return RetryConfigCustomizer.of("backendWithoutInstanceConfig",
                builder -> builder.writableStackTraceEnabled(true)
            );
        }
    }

    @Configuration
    public static class ConfigCustomizerConfiguration {
        @Bean
        public RetryConfigCustomizer defaultCustomizer() {
            return RetryConfigCustomizer.of("default",
                builder -> builder.maxAttempts(1000)
                    .writableStackTraceEnabled(true)
                    .waitDuration(Duration.ofMillis(1000))
            );
        }

        @Bean
        public RetryConfigCustomizer sharedConfigCustomizer() {
            return RetryConfigCustomizer.of("sharedConfig",
                builder -> builder.maxAttempts(2000)
                    .writableStackTraceEnabled(false)
                    .waitDuration(Duration.ofMillis(2000))
            );
        }
    }
}
