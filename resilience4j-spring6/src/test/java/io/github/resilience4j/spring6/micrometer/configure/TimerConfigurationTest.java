package io.github.resilience4j.spring6.micrometer.configure;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.micrometer.configuration.CommonTimerConfigurationProperties.InstanceProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import io.github.resilience4j.micrometer.Timer;
import io.github.resilience4j.micrometer.TimerRegistry;
import io.github.resilience4j.micrometer.event.TimerEvent;
import io.github.resilience4j.spring6.micrometer.configure.utils.FixedOnFailureTagResolver;
import io.github.resilience4j.spring6.micrometer.configure.utils.QualifiedClassNameOnFailureTagResolver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TimerConfigurationTest {

    @Test
    public void shouldConfigureInstancesUsingDedicatedConfigs() {
        InstanceProperties instanceProperties1 = new InstanceProperties()
                .setMetricNames("resilience4j.timer.operations1")
                .setOnFailureTagResolver(QualifiedClassNameOnFailureTagResolver.class)
                .setEventConsumerBufferSize(1);
        InstanceProperties instanceProperties2 = new InstanceProperties()
                .setMetricNames("resilience4j.timer.operations2")
                .setOnFailureTagResolver(FixedOnFailureTagResolver.class)
                .setEventConsumerBufferSize(10);
        TimerConfigurationProperties configurationProperties = new TimerConfigurationProperties();
        configurationProperties.getInstances().put("backend1", instanceProperties1);
        configurationProperties.getInstances().put("backend2", instanceProperties2);
        configurationProperties.setTimerAspectOrder(200);
        TimerConfiguration configuration = new TimerConfiguration();
        DefaultEventConsumerRegistry<TimerEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();
        TimerRegistry registry = configuration.timerRegistry(
                configurationProperties, eventConsumerRegistry, new CompositeRegistryEventConsumer<>(emptyList()), new CompositeCustomizer<>(emptyList()), new SimpleMeterRegistry()
        );

        assertThat(configurationProperties.getTimerAspectOrder()).isEqualTo(200);
        assertThat(registry.getAllTimers().count()).isEqualTo(2);
        Timer timer1 = registry.timer("backend1");
        assertThat(timer1).isNotNull();
        assertThat(timer1.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.operations1");
        assertThat(timer1.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);
        Timer timer2 = registry.timer("backend2");
        assertThat(timer2).isNotNull();
        assertThat(timer2.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.operations2");
        assertThat(timer2.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);
        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
    }

    @Test
    public void shouldConfigureInstancesUsingPredefinedDefaultConfig() {
        InstanceProperties instanceProperties1 = new InstanceProperties()
                .setMetricNames("resilience4j.timer.operations1");
        InstanceProperties instanceProperties2 = new InstanceProperties()
                .setOnFailureTagResolver(FixedOnFailureTagResolver.class);
        TimerConfigurationProperties configurationProperties = new TimerConfigurationProperties();
        configurationProperties.getInstances().put("backend1", instanceProperties1);
        configurationProperties.getInstances().put("backend2", instanceProperties2);
        TimerConfiguration configuration = new TimerConfiguration();
        TimerRegistry registry = configuration.timerRegistry(
                configurationProperties, new DefaultEventConsumerRegistry<>(), new CompositeRegistryEventConsumer<>(emptyList()), new CompositeCustomizer<>(emptyList()), new SimpleMeterRegistry()
        );

        assertThat(registry.getAllTimers().count()).isEqualTo(2);
        Timer timer1 = registry.timer("backend1");
        assertThat(timer1).isNotNull();
        assertThat(timer1.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.operations1");
        assertThat(timer1.getTimerConfig().getOnFailureTagResolver().apply(new RuntimeException())).isEqualTo("RuntimeException");
        Timer timer2 = registry.timer("backend2");
        assertThat(timer2).isNotNull();
        assertThat(timer2.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.calls");
        assertThat(timer2.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);
        Timer timer3 = registry.timer("backend3");
        assertThat(timer3).isNotNull();
        assertThat(timer3.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.calls");
        assertThat(timer3.getTimerConfig().getOnFailureTagResolver().apply(new RuntimeException())).isEqualTo("RuntimeException");
    }

    @Test
    public void shouldConfigureInstancesUsingCustomDefaultConfig() {
        InstanceProperties defaultProperties = new InstanceProperties()
                .setMetricNames("resilience4j.timer.default")
                .setOnFailureTagResolver(FixedOnFailureTagResolver.class);
        InstanceProperties instanceProperties1 = new InstanceProperties()
                .setMetricNames("resilience4j.timer.operations1");
        InstanceProperties instanceProperties2 = new InstanceProperties()
                .setOnFailureTagResolver(QualifiedClassNameOnFailureTagResolver.class);
        TimerConfigurationProperties configurationProperties = new TimerConfigurationProperties();
        configurationProperties.getConfigs().put("default", defaultProperties);
        configurationProperties.getInstances().put("backend1", instanceProperties1);
        configurationProperties.getInstances().put("backend2", instanceProperties2);
        TimerConfiguration configuration = new TimerConfiguration();
        TimerRegistry registry = configuration.timerRegistry(
                configurationProperties, new DefaultEventConsumerRegistry<>(), new CompositeRegistryEventConsumer<>(emptyList()), new CompositeCustomizer<>(emptyList()), new SimpleMeterRegistry()
        );

        assertThat(registry.getAllTimers().count()).isEqualTo(2);
        Timer timer1 = registry.timer("backend1");
        assertThat(timer1).isNotNull();
        assertThat(timer1.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.operations1");
        assertThat(timer1.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);
        Timer timer2 = registry.timer("backend2");
        assertThat(timer2).isNotNull();
        assertThat(timer2.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.default");
        assertThat(timer2.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);
        Timer timer3 = registry.timer("backend3");
        assertThat(timer3).isNotNull();
        assertThat(timer3.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.default");
        assertThat(timer3.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);
    }

    @Test
    public void shouldConfigureInstancesUsingCustomSharedConfig() {
        InstanceProperties sharedProperties = new InstanceProperties()
                .setMetricNames("resilience4j.timer.shared")
                .setOnFailureTagResolver(FixedOnFailureTagResolver.class);
        InstanceProperties instanceProperties1 = new InstanceProperties()
                .setMetricNames("resilience4j.timer.operations1")
                .setBaseConfig("shared");
        InstanceProperties instanceProperties2 = new InstanceProperties()
                .setOnFailureTagResolver(QualifiedClassNameOnFailureTagResolver.class)
                .setBaseConfig("shared");
        TimerConfigurationProperties configurationProperties = new TimerConfigurationProperties();
        configurationProperties.getConfigs().put("shared", sharedProperties);
        configurationProperties.getInstances().put("backend1", instanceProperties1);
        configurationProperties.getInstances().put("backend2", instanceProperties2);
        TimerConfiguration configuration = new TimerConfiguration();
        TimerRegistry registry = configuration.timerRegistry(
                configurationProperties, new DefaultEventConsumerRegistry<>(), new CompositeRegistryEventConsumer<>(emptyList()), new CompositeCustomizer<>(emptyList()), new SimpleMeterRegistry()
        );

        assertThat(registry.getAllTimers().count()).isEqualTo(2);
        Timer timer1 = registry.timer("backend1");
        assertThat(timer1).isNotNull();
        assertThat(timer1.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.operations1");
        assertThat(timer1.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(FixedOnFailureTagResolver.class);
        Timer timer2 = registry.timer("backend2");
        assertThat(timer2).isNotNull();
        assertThat(timer2.getTimerConfig().getMetricNames()).isEqualTo("resilience4j.timer.shared");
        assertThat(timer2.getTimerConfig().getOnFailureTagResolver()).isInstanceOf(QualifiedClassNameOnFailureTagResolver.class);
    }

    @Test
    public void shouldNotConfigureInstanceUsingUnknownSharedConfig() {
        InstanceProperties instanceProperties = new InstanceProperties()
                .setBaseConfig("unknown");
        TimerConfigurationProperties configurationProperties = new TimerConfigurationProperties();
        configurationProperties.getInstances().put("backend", instanceProperties);
        TimerConfiguration configuration = new TimerConfiguration();

        assertThatThrownBy(() -> configuration.timerRegistry(
                configurationProperties, new DefaultEventConsumerRegistry<>(), new CompositeRegistryEventConsumer<>(emptyList()), new CompositeCustomizer<>(emptyList()), new SimpleMeterRegistry()
        ))
                .isInstanceOf(ConfigurationNotFoundException.class)
                .hasMessage("Configuration with name 'unknown' does not exist");
    }
}
