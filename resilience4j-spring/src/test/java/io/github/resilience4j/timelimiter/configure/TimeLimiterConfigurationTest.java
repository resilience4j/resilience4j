package io.github.resilience4j.timelimiter.configure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Duration;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class TimeLimiterConfigurationTest {

    @Test
    public void testTimeLimiterRegistry() {

        // Given
        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        instanceProperties1.setTimeoutDuration(Duration.ofSeconds(3));

        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        instanceProperties2.setTimeoutDuration(Duration.ofSeconds(2));

        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties = new TimeLimiterConfigurationProperties();
        timeLimiterConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        timeLimiterConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        timeLimiterConfigurationProperties.setTimeLimiterAspectOrder(200);

        TimeLimiterConfiguration timeLimiterConfiguration = new TimeLimiterConfiguration();
        DefaultEventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        // When
        TimeLimiterRegistry timeLimiterRegistry = timeLimiterConfiguration.timeLimiterRegistry(timeLimiterConfigurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

        // Then
        assertThat(timeLimiterConfigurationProperties.getTimeLimiterAspectOrder()).isEqualTo(200);
        assertThat(timeLimiterRegistry.getAllTimeLimiters().size()).isEqualTo(2);
        TimeLimiter timeLimiter1 = timeLimiterRegistry.timeLimiter("backend1");
        assertThat(timeLimiter1).isNotNull();
        assertThat(timeLimiter1.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));

        TimeLimiter timeLimiter2 = timeLimiterRegistry.timeLimiter("backend2");
        assertThat(timeLimiter2).isNotNull();
        assertThat(timeLimiter2.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(2));

        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
    }

    @Test
    public void testCreateTimeLimiterRegistryWithSharedConfigs() {
        // Given
        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        defaultProperties.setTimeoutDuration(Duration.ofSeconds(3));
        defaultProperties.setCancelRunningFuture(true);

        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        sharedProperties.setTimeoutDuration(Duration.ofSeconds(2));
        sharedProperties.setCancelRunningFuture(false);

        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setTimeoutDuration(Duration.ofSeconds(5));

        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setCancelRunningFuture(true);

        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties = new TimeLimiterConfigurationProperties();
        timeLimiterConfigurationProperties.getConfigs().put("default", defaultProperties);
        timeLimiterConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        timeLimiterConfigurationProperties.getInstances().put("backendWithDefaultConfig", backendWithDefaultConfig);
        timeLimiterConfigurationProperties.getInstances().put("backendWithSharedConfig", backendWithSharedConfig);

        TimeLimiterConfiguration timeLimiterConfiguration = new TimeLimiterConfiguration();
        DefaultEventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        // When
        TimeLimiterRegistry timeLimiterRegistry = timeLimiterConfiguration.timeLimiterRegistry(timeLimiterConfigurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()));

        // Then
        assertThat(timeLimiterRegistry.getAllTimeLimiters().size()).isEqualTo(2);

        // Should get default config and overwrite timeout duration
        TimeLimiter timeLimiter1 = timeLimiterRegistry.timeLimiter("backendWithDefaultConfig");
        assertThat(timeLimiter1).isNotNull();
        assertThat(timeLimiter1.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
        assertThat(timeLimiter1.getTimeLimiterConfig().shouldCancelRunningFuture()).isEqualTo(true);

        // Should get shared config and overwrite cancelRunningFuture
        TimeLimiter timeLimiter2 = timeLimiterRegistry.timeLimiter("backendWithSharedConfig");
        assertThat(timeLimiter2).isNotNull();
        assertThat(timeLimiter2.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(2));
        assertThat(timeLimiter2.getTimeLimiterConfig().shouldCancelRunningFuture()).isEqualTo(true);

        // Unknown backend should get default config of Registry
        TimeLimiter timeLimiter3 = timeLimiterRegistry.timeLimiter("unknownBackend");
        assertThat(timeLimiter3).isNotNull();
        assertThat(timeLimiter3.getTimeLimiterConfig().getTimeoutDuration()).isEqualTo(Duration.ofSeconds(3));

        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
    }

    @Test
    public void testCreateTimeLimiterRegistryWithUnknownConfig() {
        TimeLimiterConfigurationProperties timeLimiterConfigurationProperties = new TimeLimiterConfigurationProperties();

        io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        timeLimiterConfigurationProperties.getInstances().put("backend", instanceProperties);

        TimeLimiterConfiguration timeLimiterConfiguration = new TimeLimiterConfiguration();
        DefaultEventConsumerRegistry<TimeLimiterEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        assertThatThrownBy(() -> timeLimiterConfiguration.timeLimiterRegistry(timeLimiterConfigurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList())))
                .isInstanceOf(ConfigurationNotFoundException.class)
                .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }
}