package io.github.resilience4j.retry.autoconfigure;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for issue #1448 - Duration property binding with various formats.
 * <p>
 * This test verifies that Duration properties can be bound from configuration using
 * various standard formats like "2s", "0.5s", "100ms", "30s", etc.
 * <p>
 * The issue was caused by a custom GenericConversionService bean that lacked Duration converters,
 * preventing Spring Boot's default Duration conversion from working properly.
 */
public class RetryDurationPropertyBindingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class));

    @Test
    public void testDurationBindingWithSecondsFormat() {
        contextRunner
            .withPropertyValues(
                "resilience4j.retry.instances.testBackend.waitDuration: 2s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);
                Retry retry = registry.retry("testBackend");

                // 2s should be parsed as 2000 milliseconds
                assertThat(retry.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(2000);
            });
    }

    @Test
    public void testDurationBindingWithMillisecondsFormat() {
        contextRunner
            .withPropertyValues(
                "resilience4j.retry.instances.testBackend.waitDuration: 500ms"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);
                Retry retry = registry.retry("testBackend");

                // 500ms should be parsed as 500 milliseconds
                assertThat(retry.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(500);
            });
    }

    @Test
    public void testDurationBindingWithDecimalSecondsFormat() {
        contextRunner
            .withPropertyValues(
                "resilience4j.retry.instances.testBackend.waitDuration: 0.5s"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);
                Retry retry = registry.retry("testBackend");

                // 0.5s should be parsed as 500 milliseconds
                assertThat(retry.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(500);
            });
    }

    @Test
    public void testDurationBindingWithPlainNumber() {
        contextRunner
            .withPropertyValues(
                "resilience4j.retry.instances.testBackend.waitDuration: 1500"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);
                Retry retry = registry.retry("testBackend");

                // Plain number should be treated as milliseconds
                assertThat(retry.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(1500);
            });
    }

    @Test
    public void testDurationBindingWithISO8601Format() {
        contextRunner
            .withPropertyValues(
                "resilience4j.retry.instances.testBackend.waitDuration: PT3S"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);
                Retry retry = registry.retry("testBackend");

                // PT3S (ISO-8601) should be parsed as 3000 milliseconds
                assertThat(retry.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(3000);
            });
    }

    @Test
    public void testMultipleInstancesWithDifferentDurationFormats() {
        contextRunner
            .withPropertyValues(
                "resilience4j.retry.instances.backend1.waitDuration: 1s",
                "resilience4j.retry.instances.backend2.waitDuration: 750ms",
                "resilience4j.retry.instances.backend3.waitDuration: 2500"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(RetryRegistry.class);
                RetryRegistry registry = context.getBean(RetryRegistry.class);

                Retry retry1 = registry.retry("backend1");
                assertThat(retry1.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(1000);

                Retry retry2 = registry.retry("backend2");
                assertThat(retry2.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(750);

                Retry retry3 = registry.retry("backend3");
                assertThat(retry3.getRetryConfig().getIntervalBiFunction().apply(0, null))
                    .isEqualTo(2500);
            });
    }
}
