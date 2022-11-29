package io.github.resilience4j.spring6.bulkhead.configure;

import io.github.resilience4j.spring6.TestThreadLocalContextPropagator;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.spring6.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.CommonThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import io.github.resilience4j.core.ContextPropagator;
import io.github.resilience4j.core.registry.CompositeRegistryEventConsumer;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test custom init of bulkhead configuration
 */
public class BulkHeadConfigurationTest {

    @Test
    public void tesFixedThreadPoolBulkHeadRegistry() {
        //Given
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties1 = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties1.setCoreThreadPoolSize(1);
        backendProperties1.setContextPropagators(TestThreadLocalContextPropagator.class);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties2 = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties2.setCoreThreadPoolSize(2);

        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getBackends().put("backend1", backendProperties1);
        bulkheadConfigurationProperties.getBackends().put("backend2", backendProperties2);

        ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();
        DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        ThreadPoolBulkheadRegistry bulkheadRegistry = threadPoolBulkheadConfiguration
            .threadPoolBulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()),
                new CompositeCustomizer<>(Collections.emptyList()));

        //Then
        assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);
        ThreadPoolBulkhead bulkhead1 = bulkheadRegistry.bulkhead("backend1");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(1);
        assertThat(bulkhead1.getBulkheadConfig().getContextPropagator()).isNotNull();
        assertThat(bulkhead1.getBulkheadConfig().getContextPropagator().size()).isEqualTo(1);
        assertThat(bulkhead1.getBulkheadConfig().getContextPropagator().get(0).getClass())
            .isEqualTo(TestThreadLocalContextPropagator.class);

        ThreadPoolBulkhead bulkhead2 = bulkheadRegistry.bulkhead("backend2");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkhead2.getBulkheadConfig().getContextPropagator()).isNotNull();
        assertThat(bulkhead2.getBulkheadConfig().getContextPropagator()).isEmpty();

        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
    }

    @Test
    public void testCreateThreadPoolBulkHeadRegistryWithSharedConfigs() {
        //Given
        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setCoreThreadPoolSize(1);
        defaultProperties.setQueueCapacity(1);
        defaultProperties.setKeepAliveDuration(Duration.ofNanos(5));
        defaultProperties.setMaxThreadPoolSize(10);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedProperties = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setCoreThreadPoolSize(2);
        sharedProperties.setQueueCapacity(2);
        sharedProperties.setContextPropagators(TestThreadLocalContextPropagator.class);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setCoreThreadPoolSize(3);

        CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setCoreThreadPoolSize(4);
        backendWithSharedConfig.setContextPropagators(TestThreadLocalContextPropagator.class);

        CommonThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getBackends()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        bulkheadConfigurationProperties.getBackends()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        ThreadPoolBulkheadConfiguration threadPoolBulkheadConfiguration = new ThreadPoolBulkheadConfiguration();
        DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        try {
            ThreadPoolBulkheadRegistry bulkheadRegistry = threadPoolBulkheadConfiguration
                .threadPoolBulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry,
                    new CompositeRegistryEventConsumer<>(emptyList()),
                    new CompositeCustomizer<>(Collections.emptyList()));
            //Then
            assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);
            // Should get default config and core number
            ThreadPoolBulkhead bulkhead1 = bulkheadRegistry.bulkhead("backendWithDefaultConfig");
            assertThat(bulkhead1).isNotNull();
            assertThat(bulkhead1.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(3);
            assertThat(bulkhead1.getBulkheadConfig().getQueueCapacity()).isEqualTo(1);
            assertThat(bulkhead1.getBulkheadConfig().getContextPropagator()).isNotNull();
            assertThat(bulkhead1.getBulkheadConfig().getContextPropagator()).isEmpty();

            // Should get shared config and overwrite core number
            ThreadPoolBulkhead bulkhead2 = bulkheadRegistry.bulkhead("backendWithSharedConfig");
            assertThat(bulkhead2).isNotNull();
            assertThat(bulkhead2.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(4);
            assertThat(bulkhead2.getBulkheadConfig().getQueueCapacity()).isEqualTo(2);
            assertThat(bulkhead2.getBulkheadConfig().getContextPropagator()).isNotNull();
            assertThat(bulkhead2.getBulkheadConfig().getContextPropagator().size()).isEqualTo(2);
            List<Class<? extends ContextPropagator>> ctxPropagators = bulkhead2
                .getBulkheadConfig().getContextPropagator().stream().map(ctx -> ctx.getClass())
                .collect(
                    Collectors.toList());
            assertThat(ctxPropagators).containsExactlyInAnyOrder(TestThreadLocalContextPropagator.class,
                TestThreadLocalContextPropagator.class);


            // Unknown backend should get default config of Registry
            ThreadPoolBulkhead bulkhead3 = bulkheadRegistry.bulkhead("unknownBackend");
            assertThat(bulkhead3).isNotNull();
            assertThat(bulkhead3.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(1);
            assertThat(bulkhead3.getBulkheadConfig().getContextPropagator()).isNotNull();
            assertThat(bulkhead3.getBulkheadConfig().getContextPropagator()).isEmpty();

            assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
        } catch (Exception e) {
            System.out.println(
                "exception in testCreateThreadPoolBulkHeadRegistryWithSharedConfigs():" + e);
        }
    }


    @Test
    public void testBulkHeadRegistry() {
        //Given
        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties1 = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        instanceProperties1.setMaxConcurrentCalls(3);
        assertThat(instanceProperties1.getEventConsumerBufferSize()).isNull();

        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties2 = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        instanceProperties2.setMaxConcurrentCalls(2);
        assertThat(instanceProperties2.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        bulkheadConfigurationProperties.getInstances().put("backend2", instanceProperties2);

        BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
        DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        BulkheadRegistry bulkheadRegistry = bulkheadConfiguration
            .bulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()), new CompositeCustomizer<>(
                    Collections.emptyList()));

        //Then
        assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);
        Bulkhead bulkhead1 = bulkheadRegistry.bulkhead("backend1");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);

        Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("backend2");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);

        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(2);
    }

    @Test
    public void testCreateBulkHeadRegistryWithSharedConfigs() {
        //Given
        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxConcurrentCalls(3);
        defaultProperties.setMaxWaitDuration(Duration.ofMillis(50L));
        assertThat(defaultProperties.getEventConsumerBufferSize()).isNull();

        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties sharedProperties = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxConcurrentCalls(2);
        sharedProperties.setMaxWaitDuration(Duration.ofMillis(100L));
        assertThat(sharedProperties.getEventConsumerBufferSize()).isNull();

        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setMaxWaitDuration(Duration.ofMillis(200L));
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();

        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setMaxWaitDuration(Duration.ofMillis(300L));
        assertThat(backendWithSharedConfig.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getInstances()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        bulkheadConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
        DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        BulkheadRegistry bulkheadRegistry = bulkheadConfiguration
            .bulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()),
                new CompositeCustomizer<>(Collections.emptyList()));

        //Then
        assertThat(bulkheadRegistry.getAllBulkheads().size()).isEqualTo(2);

        // Should get default config and overwrite max calls and wait time
        Bulkhead bulkhead1 = bulkheadRegistry.bulkhead("backendWithDefaultConfig");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead1.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(200L);

        // Should get shared config and overwrite wait time
        Bulkhead bulkhead2 = bulkheadRegistry.bulkhead("backendWithSharedConfig");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead2.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(300L);

        // Unknown backend should get default config of Registry
        Bulkhead bulkhead3 = bulkheadRegistry.bulkhead("unknownBackend");
        assertThat(bulkhead3).isNotNull();
        assertThat(bulkhead3.getBulkheadConfig().getMaxWaitDuration().toMillis()).isEqualTo(50L);

        assertThat(eventConsumerRegistry.getAllEventConsumer()).hasSize(3);
    }

    @Test
    public void testCreateBulkHeadRegistryWithUnknownConfig() {
        BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();

        io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties = new io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        bulkheadConfigurationProperties.getInstances().put("backend", instanceProperties);

        BulkheadConfiguration bulkheadConfiguration = new BulkheadConfiguration();
        DefaultEventConsumerRegistry<BulkheadEvent> eventConsumerRegistry = new DefaultEventConsumerRegistry<>();

        //When
        assertThatThrownBy(() -> bulkheadConfiguration
            .bulkheadRegistry(bulkheadConfigurationProperties, eventConsumerRegistry,
                new CompositeRegistryEventConsumer<>(emptyList()),
                new CompositeCustomizer<>(Collections.emptyList())))
            .isInstanceOf(ConfigurationNotFoundException.class)
            .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }

}