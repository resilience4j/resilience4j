/*
 * Copyright 2019 Mahmoud Romeh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.common.bulkhead.configuration;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.core.ConfigurationNotFoundException;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * unit test for bulkhead properties
 */
public class BulkheadConfigurationPropertiesTest {

    @Test
    public void tesFixedThreadPoolBulkHeadProperties() {
        //Given
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties1 = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties1.setCoreThreadPoolSize(1);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendProperties2 = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendProperties2.setCoreThreadPoolSize(2);

        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new ThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getBackends().put("backend1", backendProperties1);
        bulkheadConfigurationProperties.getBackends().put("backend2", backendProperties2);
        Map<String, String> tags = new HashMap<>();
        tags.put("testKey1", "testKet2");
        bulkheadConfigurationProperties.setTags(tags);

        //Then
        assertThat(bulkheadConfigurationProperties.getTags()).isNotEmpty();
        assertThat(bulkheadConfigurationProperties.getBackends().size()).isEqualTo(2);
        assertThat(bulkheadConfigurationProperties.getInstances().size()).isEqualTo(2);
        ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend1", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(1);

        ThreadPoolBulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createThreadPoolBulkheadConfig("backend2", compositeThreadPoolBulkheadCustomizer());
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getCoreThreadPoolSize()).isEqualTo(2);

    }

    @Test
    public void testCreateThreadPoolBulkHeadPropertiesWithSharedConfigs() {
        //Given
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setCoreThreadPoolSize(1);
        defaultProperties.setQueueCapacity(1);
        defaultProperties.setKeepAliveDuration(Duration.ofMillis(5));
        defaultProperties.setMaxThreadPoolSize(10);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties sharedProperties = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setCoreThreadPoolSize(2);
        sharedProperties.setQueueCapacity(2);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setCoreThreadPoolSize(3);

        ThreadPoolBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setCoreThreadPoolSize(4);

        ThreadPoolBulkheadConfigurationProperties bulkheadConfigurationProperties = new ThreadPoolBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getBackends()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        bulkheadConfigurationProperties.getBackends()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //When
        //Then
        try {
            assertThat(bulkheadConfigurationProperties.getBackends().size()).isEqualTo(2);
            // Should get default config and core number
            ThreadPoolBulkheadConfig bulkhead1 = bulkheadConfigurationProperties
                .createThreadPoolBulkheadConfig("backendWithDefaultConfig",
                    compositeThreadPoolBulkheadCustomizer());
            assertThat(bulkhead1).isNotNull();
            assertThat(bulkhead1.getCoreThreadPoolSize()).isEqualTo(3);
            assertThat(bulkhead1.getQueueCapacity()).isEqualTo(1);
            // Should get shared config and overwrite core number
            ThreadPoolBulkheadConfig bulkhead2 = bulkheadConfigurationProperties
                .createThreadPoolBulkheadConfig("backendWithSharedConfig",
                    compositeThreadPoolBulkheadCustomizer());
            assertThat(bulkhead2).isNotNull();
            assertThat(bulkhead2.getCoreThreadPoolSize()).isEqualTo(4);
            assertThat(bulkhead2.getQueueCapacity()).isEqualTo(2);
            // Unknown backend should get default config of Registry
            ThreadPoolBulkheadConfig bulkhead3 = bulkheadConfigurationProperties
                .createThreadPoolBulkheadConfig("unknownBackend",
                    compositeThreadPoolBulkheadCustomizer());
            assertThat(bulkhead3).isNotNull();
            assertThat(bulkhead3.getCoreThreadPoolSize())
                .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE);
        } catch (Exception e) {
            System.out.println(
                "exception in testCreateThreadPoolBulkHeadRegistryWithSharedConfigs():" + e);
        }

    }


    @Test
    public void testBulkHeadProperties() {
        //Given
        BulkheadConfigurationProperties.InstanceProperties instanceProperties1 = new BulkheadConfigurationProperties.InstanceProperties();
        instanceProperties1.setMaxConcurrentCalls(3);
        assertThat(instanceProperties1.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties.InstanceProperties instanceProperties2 = new BulkheadConfigurationProperties.InstanceProperties();
        instanceProperties2.setMaxConcurrentCalls(2);
        assertThat(instanceProperties2.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        bulkheadConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        Map<String, String> globalTags = new HashMap<>();
        globalTags.put("testKey1", "testKet2");
        bulkheadConfigurationProperties.setTags(globalTags);
        //Then
        assertThat(bulkheadConfigurationProperties.getInstances().size()).isEqualTo(2);
        assertThat(bulkheadConfigurationProperties.getTags()).isNotEmpty();
        BulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createBulkheadConfig(instanceProperties1, compositeBulkheadCustomizer(), "backend1");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getMaxConcurrentCalls()).isEqualTo(3);

        BulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createBulkheadConfig(instanceProperties2, compositeBulkheadCustomizer(), "backend2");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getMaxConcurrentCalls()).isEqualTo(2);


    }

    @Test
    public void testCreateBulkHeadPropertiesWithSharedConfigs() {
        //Given
        BulkheadConfigurationProperties.InstanceProperties defaultProperties = new BulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxConcurrentCalls(3);
        defaultProperties.setMaxWaitDuration(Duration.ofMillis(50));
        assertThat(defaultProperties.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties.InstanceProperties sharedProperties = new BulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxConcurrentCalls(2);
        sharedProperties.setMaxWaitDuration(Duration.ofMillis(100L));
        assertThat(sharedProperties.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new BulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("default");
        backendWithDefaultConfig.setMaxWaitDuration(Duration.ofMillis(200L));
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();

        BulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new BulkheadConfigurationProperties.InstanceProperties();
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

        //Then
        assertThat(bulkheadConfigurationProperties.getInstances().size()).isEqualTo(2);

        // Should get default config and overwrite max calls and wait time
        BulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createBulkheadConfig(backendWithDefaultConfig, compositeBulkheadCustomizer(),
                "backendWithDefaultConfig");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead1.getMaxWaitDuration().toMillis()).isEqualTo(200L);

        // Should get shared config and overwrite wait time
        BulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createBulkheadConfig(backendWithSharedConfig, compositeBulkheadCustomizer(),
                "backendWithSharedConfig");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead2.getMaxWaitDuration().toMillis()).isEqualTo(300L);

        // Unknown backend should get default config of Registry
        BulkheadConfig bulkhead3 = bulkheadConfigurationProperties
            .createBulkheadConfig(new BulkheadConfigurationProperties.InstanceProperties(),
                compositeBulkheadCustomizer(), "unknown");
        assertThat(bulkhead3).isNotNull();
        assertThat(bulkhead3.getMaxWaitDuration().toMillis()).isEqualTo(0L);

    }

    @Test
    public void testCreateBulkHeadPropertiesWithUnknownConfig() {
        BulkheadConfigurationProperties bulkheadConfigurationProperties = new BulkheadConfigurationProperties();

        BulkheadConfigurationProperties.InstanceProperties instanceProperties = new BulkheadConfigurationProperties.InstanceProperties();
        instanceProperties.setBaseConfig("unknownConfig");
        bulkheadConfigurationProperties.getInstances().put("backend", instanceProperties);

        //When
        assertThatThrownBy(
            () -> bulkheadConfigurationProperties.createBulkheadConfig(instanceProperties,
                compositeBulkheadCustomizer(), "unknownConfig"))
            .isInstanceOf(ConfigurationNotFoundException.class)
            .hasMessage("Configuration with name 'unknownConfig' does not exist");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMaxConcurrentCalls() {
        BulkheadConfigurationProperties.InstanceProperties defaultProperties = new BulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxConcurrentCalls(-100);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMaxWaitDuration() {
        BulkheadConfigurationProperties.InstanceProperties defaultProperties = new BulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxWaitDuration(Duration.ofMillis(-1000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBulkheadIllegalArgumentOnEventConsumerBufferSize() {
        BulkheadConfigurationProperties.InstanceProperties defaultProperties = new BulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThreadPoolBulkheadIllegalArgumentOnEventConsumerBufferSize() {
        ThreadPoolBulkheadConfigurationProperties.InstanceProperties defaultProperties = new ThreadPoolBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(-1);
    }

    private CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }

    private CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }

}