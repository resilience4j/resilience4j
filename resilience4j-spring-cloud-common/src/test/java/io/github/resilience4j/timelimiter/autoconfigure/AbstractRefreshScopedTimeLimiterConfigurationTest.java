package io.github.resilience4j.timelimiter.autoconfigure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfigurationProperties;
import org.junit.Test;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractRefreshScopedTimeLimiterConfigurationTest {

    @Test
    public void testRefreshScopedTimeLimiterConfig() {
        Arrays.stream(AbstractRefreshScopedTimeLimiterConfiguration.class.getMethods())
            .filter(method -> method.isAnnotationPresent(Bean.class))
            .forEach(method -> assertThat(method.isAnnotationPresent(RefreshScope.class)).isTrue());
    }

    @Test
    public void testTimeLimiterCloudCommonConfig() {
        TimeLimiterConfig timeLimiterConfig = new TimeLimiterConfig();

        assertThat(timeLimiterConfig.timeLimiterRegistry(
            new TimeLimiterConfigurationProperties(), new DefaultEventConsumerRegistry<>(),
            new CompositeRegistryEventConsumer<>(Collections.emptyList()),
            new CompositeCustomizer<>(Collections.emptyList()))).isNotNull();
    }


    static class TimeLimiterConfig extends AbstractRefreshScopedTimeLimiterConfiguration {

    }
}