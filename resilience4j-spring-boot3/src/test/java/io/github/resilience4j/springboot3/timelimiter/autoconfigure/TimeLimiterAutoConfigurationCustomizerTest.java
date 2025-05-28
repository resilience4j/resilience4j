package io.github.resilience4j.springboot3.timelimiter.autoconfigure;

import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests combinations of config properties ({@code resilience4j.timelimiter.configs.<name>.*}),
 * instance properties ({@code resilience4j.timelimiter.instances.<name>.*}) and {@link TimeLimiterConfigCustomizer}.
 * <p>
 * To make this test easier to follow it always uses different magnitude of values for different ways to configure a time limiter:
 * <ul>
 *     <li>config properties - N * 10</li>
 *     <li>instance properties - N * 100</li>
 *     <li>customizer - N * 1000</li>
 * </ul>
 * where N is index of the config. This way when asserting value {@code 200} it is guaranteed to be coming from instance properties.
 */
public class TimeLimiterAutoConfigurationCustomizerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(TimeLimiterAutoConfiguration.class))
        .withPropertyValues(
            "resilience4j.timelimiter.configs.default.cancelRunningFuture: true",
            "resilience4j.timelimiter.configs.default.timeoutDuration: 10ms",
            "resilience4j.timelimiter.configs.sharedConfig.cancelRunningFuture: false",
            "resilience4j.timelimiter.configs.sharedConfig.timeoutDuration: 20ms"
        );

    @Test
    public void testUserConfigShouldBeAbleToProvideCustomizers() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.timelimiter.instances.backendWithoutSharedConfig.cancelRunningFuture: false",
                "resilience4j.timelimiter.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.timelimiter.instances.backendWithSharedConfig.cancelRunningFuture: true"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(TimeLimiterRegistry.class);
                TimeLimiterRegistry registry = context.getBean(TimeLimiterRegistry.class);

                TimeLimiterConfig backendWithoutSharedConfig = registry.timeLimiter("backendWithoutSharedConfig").getTimeLimiterConfig();
                // from instance config
                assertThat(backendWithoutSharedConfig.shouldCancelRunningFuture()).isEqualTo(false);
                // from customizer
                assertThat(backendWithoutSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));

                TimeLimiterConfig backendWithSharedConfig = registry.timeLimiter("backendWithSharedConfig").getTimeLimiterConfig();
                // from instance config
                assertThat(backendWithSharedConfig.shouldCancelRunningFuture()).isEqualTo(true);
                // from customizer
                assertThat(backendWithSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(2000));

                TimeLimiterConfig backendWithoutInstanceConfig = registry.timeLimiter("backendWithoutInstanceConfig").getTimeLimiterConfig();
                // from default config
                assertThat(backendWithoutInstanceConfig.shouldCancelRunningFuture()).isEqualTo(true);
                // from customizer
                assertThat(backendWithoutInstanceConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(3000));
            });
    }

    @Test
    public void testCustomizersShouldOverrideProperties() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.timelimiter.instances.backendWithoutSharedConfig.timeoutDuration: 100"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(TimeLimiterRegistry.class);
                TimeLimiterRegistry registry = context.getBean(TimeLimiterRegistry.class);

                TimeLimiterConfig backendWithoutSharedConfig = registry.timeLimiter("backendWithoutSharedConfig").getTimeLimiterConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.shouldCancelRunningFuture()).isEqualTo(true);
                // from customizer
                assertThat(backendWithoutSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));
            });
    }

    @Test
    public void testCustomizersAreAppliedOnConfigs() {
        // Given
        contextRunner.withUserConfiguration(ConfigCustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.timelimiter.instances.backendWithoutSharedConfig.cancelRunningFuture: false",
                "resilience4j.timelimiter.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.timelimiter.instances.backendWithSharedConfig.cancelRunningFuture: true"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(TimeLimiterRegistry.class);
                TimeLimiterRegistry registry = context.getBean(TimeLimiterRegistry.class);

                TimeLimiterConfig defaultConfig = registry.getConfiguration("default").orElseThrow();
                // from customizer
                assertThat(defaultConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));
                // from customizer
                assertThat(defaultConfig.shouldCancelRunningFuture()).isEqualTo(true);

                TimeLimiterConfig backendWithoutSharedConfig = registry.timeLimiter("backendWithoutSharedConfig").getTimeLimiterConfig();
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));
                // from instance config
                assertThat(backendWithoutSharedConfig.shouldCancelRunningFuture()).isEqualTo(false);


                TimeLimiterConfig backendWithSharedConfig = registry.timeLimiter("backendWithSharedConfig").getTimeLimiterConfig();
                // from shared config customizer
                assertThat(backendWithSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(2000));
                // from instance config
                assertThat(backendWithSharedConfig.shouldCancelRunningFuture()).isEqualTo(true);

                TimeLimiterConfig backendWithoutInstanceConfig = registry.timeLimiter("backendWithoutInstanceConfig").getTimeLimiterConfig();
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.shouldCancelRunningFuture()).isEqualTo(true);
            });
    }

    @Configuration
    public static class CustomizerConfiguration {
        @Bean
        public TimeLimiterConfigCustomizer backendWithoutSharedConfigCustomizer() {
            return TimeLimiterConfigCustomizer.of("backendWithoutSharedConfig",
                builder -> builder.timeoutDuration(Duration.ofMillis(1000))
            );
        }

        @Bean
        public TimeLimiterConfigCustomizer backendWithSharedConfigCustomizer() {
            return TimeLimiterConfigCustomizer.of("backendWithSharedConfig",
                builder -> builder.timeoutDuration(Duration.ofMillis(2000))
            );
        }

        @Bean
        public TimeLimiterConfigCustomizer backendWithoutInstanceConfigCustomizer() {
            return TimeLimiterConfigCustomizer.of("backendWithoutInstanceConfig",
                builder -> builder.timeoutDuration(Duration.ofMillis(3000))
            );
        }
    }

    @Configuration
    public static class ConfigCustomizerConfiguration {
        @Bean
        public TimeLimiterConfigCustomizer defaultCustomizer() {
            return TimeLimiterConfigCustomizer.of("default",
                builder -> builder.cancelRunningFuture(true)
                    .timeoutDuration(Duration.ofMillis(1000))
            );
        }

        @Bean
        public TimeLimiterConfigCustomizer sharedConfigCustomizer() {
            return TimeLimiterConfigCustomizer.of("sharedConfig",
                builder -> builder.cancelRunningFuture(false)
                    .timeoutDuration(Duration.ofMillis(2000))
            );
        }
    }
}
