package io.github.resilience4j.springboot3.ratelimiter.autoconfigure;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests combinations of config properties ({@code resilience4j.ratelimiter.configs.<name>.*}),
 * instance properties ({@code resilience4j.ratelimiter.instances.<name>.*}) and {@link RateLimiterConfigCustomizer}.
 * <p>
 * To make this test easier to follow it always uses different magnitude of values for different ways to configure a rate limiter:
 * <ul>
 *     <li>config properties - N * 10</li>
 *     <li>instance properties - N * 100</li>
 *     <li>customizer - N * 1000</li>
 * </ul>
 * where N is index of the config. This way when asserting value {@code 200} it is guaranteed to be coming from instance properties.
 */
public class RateLimiterAutoConfigurationCustomizerTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RateLimiterAutoConfiguration.class))
        .withPropertyValues(
            "resilience4j.ratelimiter.configs.default.limitForPeriod: 10",
            "resilience4j.ratelimiter.configs.default.timeoutDuration: 10ms",
            "resilience4j.ratelimiter.configs.default.limitRefreshPeriod: 10ms",
            "resilience4j.ratelimiter.configs.sharedConfig.limitForPeriod: 20",
            "resilience4j.ratelimiter.configs.sharedConfig.timeoutDuration: 20ms",
            "resilience4j.ratelimiter.configs.sharedConfig.limitRefreshPeriod: 20ms"
        );

    @Test
    public void testUserConfigShouldBeAbleToProvideCustomizers() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.ratelimiter.instances.backendWithoutSharedConfig.limitForPeriod: 100",
                "resilience4j.ratelimiter.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.ratelimiter.instances.backendWithSharedConfig.limitForPeriod: 200"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(RateLimiterRegistry.class);
                RateLimiterRegistry registry = context.getBean(RateLimiterRegistry.class);

                RateLimiterConfig backendWithoutSharedConfig = registry.rateLimiter("backendWithoutSharedConfig").getRateLimiterConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(10));
                // from instance config
                assertThat(backendWithoutSharedConfig.getLimitForPeriod()).isEqualTo(100);
                // from customizer
                assertThat(backendWithoutSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));

                RateLimiterConfig backendWithSharedConfig = registry.rateLimiter("backendWithSharedConfig").getRateLimiterConfig();
                // from default config
                assertThat(backendWithSharedConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(20));
                // from instance config
                assertThat(backendWithSharedConfig.getLimitForPeriod()).isEqualTo(200);
                // from customizer
                assertThat(backendWithSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(2000));

                RateLimiterConfig backendWithoutInstanceConfig = registry.rateLimiter("backendWithoutInstanceConfig").getRateLimiterConfig();
                // from default config
                assertThat(backendWithoutInstanceConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(10));
                // from default config
                assertThat(backendWithoutInstanceConfig.getLimitForPeriod()).isEqualTo(10);
                // from customizer
                assertThat(backendWithoutInstanceConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(3000));
            });
    }

    @Test
    public void testCustomizersShouldOverrideProperties() {
        // Given
        contextRunner.withUserConfiguration(CustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.ratelimiter.instances.backendWithoutSharedConfig.timeoutDuration: 100ms"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(RateLimiterRegistry.class);
                RateLimiterRegistry registry = context.getBean(RateLimiterRegistry.class);

                RateLimiterConfig backendWithoutSharedConfig = registry.rateLimiter("backendWithoutSharedConfig").getRateLimiterConfig();
                // from default config
                assertThat(backendWithoutSharedConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(10));
                // from default config
                assertThat(backendWithoutSharedConfig.getLimitForPeriod()).isEqualTo(10);
                // from customizer
                assertThat(backendWithoutSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));
            });
    }

    @Test
    public void testCustomizersAreAppliedOnConfigs() {
        // Given
        contextRunner.withUserConfiguration(ConfigCustomizerConfiguration.class)
            .withPropertyValues(
                "resilience4j.ratelimiter.instances.backendWithoutSharedConfig.limitForPeriod: 100",
                "resilience4j.ratelimiter.instances.backendWithSharedConfig.baseConfig: sharedConfig",
                "resilience4j.ratelimiter.instances.backendWithSharedConfig.limitForPeriod: 200"
            )
            .run(context -> {
                // Then
                assertThat(context).hasSingleBean(RateLimiterRegistry.class);
                RateLimiterRegistry registry = context.getBean(RateLimiterRegistry.class);

                RateLimiterConfig defaultConfig = registry.getConfiguration("default").orElseThrow();
                // from customizer
                assertThat(defaultConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(1000));
                // from customizer
                assertThat(defaultConfig.getLimitForPeriod()).isEqualTo(1000);
                // from customizer
                assertThat(defaultConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));

                RateLimiterConfig backendWithoutSharedConfig = registry.rateLimiter("backendWithoutSharedConfig").getRateLimiterConfig();
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(1000));
                // from instance config
                assertThat(backendWithoutSharedConfig.getLimitForPeriod()).isEqualTo(100);
                // from default config customizer
                assertThat(backendWithoutSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));


                RateLimiterConfig backendWithSharedConfig = registry.rateLimiter("backendWithSharedConfig").getRateLimiterConfig();
                // from shared config customizer
                assertThat(backendWithSharedConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(2000));
                // from instance config
                assertThat(backendWithSharedConfig.getLimitForPeriod()).isEqualTo(200);
                // from shared config customizer
                assertThat(backendWithSharedConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(2000));

                RateLimiterConfig backendWithoutInstanceConfig = registry.rateLimiter("backendWithoutInstanceConfig").getRateLimiterConfig();
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getLimitRefreshPeriod()).isEqualTo(Duration.ofMillis(1000));
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getLimitForPeriod()).isEqualTo(1000);
                // from default config customizer
                assertThat(backendWithoutInstanceConfig.getTimeoutDuration()).isEqualTo(Duration.ofMillis(1000));
            });
    }

    @Configuration
    public static class CustomizerConfiguration {
        @Bean
        public RateLimiterConfigCustomizer backendWithoutSharedConfigCustomizer() {
            return RateLimiterConfigCustomizer.of("backendWithoutSharedConfig",
                builder -> builder.timeoutDuration(Duration.ofMillis(1000))
            );
        }

        @Bean
        public RateLimiterConfigCustomizer backendWithSharedConfigCustomizer() {
            return RateLimiterConfigCustomizer.of("backendWithSharedConfig",
                    builder -> builder.timeoutDuration(Duration.ofMillis(2000))
            );
        }

        @Bean
        public RateLimiterConfigCustomizer backendWithoutInstanceConfigCustomizer() {
            return RateLimiterConfigCustomizer.of("backendWithoutInstanceConfig",
                builder -> builder.timeoutDuration(Duration.ofMillis(3000))
            );
        }
    }

    @Configuration
    public static class ConfigCustomizerConfiguration {
        @Bean
        public RateLimiterConfigCustomizer defaultCustomizer() {
            return RateLimiterConfigCustomizer.of("default",
                builder -> builder.limitForPeriod(1000)
                    .timeoutDuration(Duration.ofMillis(1000))
                    .limitRefreshPeriod(Duration.ofMillis(1000))
            );
        }

        @Bean
        public RateLimiterConfigCustomizer sharedConfigCustomizer() {
            return RateLimiterConfigCustomizer.of("sharedConfig",
                builder -> builder.limitForPeriod(2000)
                    .timeoutDuration(Duration.ofMillis(2000))
                    .limitRefreshPeriod(Duration.ofMillis(2000))
            );
        }
    }
}
