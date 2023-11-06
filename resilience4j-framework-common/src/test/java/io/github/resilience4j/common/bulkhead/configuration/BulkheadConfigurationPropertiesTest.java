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
    public void testBulkHeadProperties() {
        //Given
        CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties1 = new CommonBulkheadConfigurationProperties.InstanceProperties();
        instanceProperties1.setMaxConcurrentCalls(3);
        instanceProperties1.setWritableStackTraceEnabled(true);
        assertThat(instanceProperties1.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties2 = new CommonBulkheadConfigurationProperties.InstanceProperties();
        instanceProperties2.setMaxConcurrentCalls(2);
        instanceProperties2.setWritableStackTraceEnabled(false);
        assertThat(instanceProperties2.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getInstances().put("backend1", instanceProperties1);
        bulkheadConfigurationProperties.getInstances().put("backend2", instanceProperties2);
        Map<String, String> globalTags = new HashMap<>();
        globalTags.put("testKey1", "testKet2");
        bulkheadConfigurationProperties.setTags(globalTags);
        //Then
        assertThat(bulkheadConfigurationProperties.getInstances()).hasSize(2);
        assertThat(bulkheadConfigurationProperties.getTags()).isNotEmpty();
        BulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createBulkheadConfig(instanceProperties1, compositeBulkheadCustomizer(), "backend1");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead1.isWritableStackTraceEnabled()).isTrue();

        BulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createBulkheadConfig(instanceProperties2, compositeBulkheadCustomizer(), "backend2");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead2.isWritableStackTraceEnabled()).isFalse();


    }

    @Test
    public void testCreateBulkHeadPropertiesWithSharedConfigs() {
        //Given
        CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxConcurrentCalls(3);
        defaultProperties.setMaxWaitDuration(Duration.ofMillis(50));
        defaultProperties.setWritableStackTraceEnabled(true);
        assertThat(defaultProperties.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties sharedProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxConcurrentCalls(2);
        sharedProperties.setMaxWaitDuration(Duration.ofMillis(100L));
        sharedProperties.setWritableStackTraceEnabled(false);
        assertThat(sharedProperties.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties backendWithDefaultConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        backendWithDefaultConfig.setBaseConfig("defaultConfig");
        backendWithDefaultConfig.setMaxWaitDuration(Duration.ofMillis(200L));
        backendWithDefaultConfig.setWritableStackTraceEnabled(true);
        assertThat(backendWithDefaultConfig.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setMaxWaitDuration(Duration.ofMillis(300L));
        backendWithSharedConfig.setWritableStackTraceEnabled(false);
        assertThat(backendWithSharedConfig.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("defaultConfig", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getInstances()
            .put("backendWithDefaultConfig", backendWithDefaultConfig);
        bulkheadConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //Then
        assertThat(bulkheadConfigurationProperties.getInstances()).hasSize(2);

        // Should get default config and overwrite max calls and wait time
        BulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createBulkheadConfig(backendWithDefaultConfig, compositeBulkheadCustomizer(),
                "backendWithDefaultConfig");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead1.getMaxWaitDuration().toMillis()).isEqualTo(200L);
        assertThat(bulkhead1.isWritableStackTraceEnabled()).isTrue();

        // Should get shared config and overwrite wait time
        BulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createBulkheadConfig(backendWithSharedConfig, compositeBulkheadCustomizer(),
                "backendWithSharedConfig");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead2.getMaxWaitDuration().toMillis()).isEqualTo(300L);
        assertThat(bulkhead2.isWritableStackTraceEnabled()).isFalse();

        // Unknown backend should get default config of Registry
        BulkheadConfig bulkhead3 = bulkheadConfigurationProperties
            .createBulkheadConfig(new CommonBulkheadConfigurationProperties.InstanceProperties(),
                compositeBulkheadCustomizer(), "unknown");
        assertThat(bulkhead3).isNotNull();
        assertThat(bulkhead3.getMaxWaitDuration().toMillis()).isEqualTo(0L);
        assertThat(bulkhead3.isWritableStackTraceEnabled()).isTrue();

    }

    @Test
    public void testCreateBulkHeadPropertiesWithDefaultConfig() {
        //Given
        CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxConcurrentCalls(3);
        defaultProperties.setMaxWaitDuration(Duration.ofMillis(50));
        defaultProperties.setWritableStackTraceEnabled(true);
        assertThat(defaultProperties.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties sharedProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        sharedProperties.setMaxConcurrentCalls(2);
        sharedProperties.setMaxWaitDuration(Duration.ofMillis(100L));
        sharedProperties.setWritableStackTraceEnabled(false);
        assertThat(sharedProperties.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        backendWithoutBaseConfig.setMaxWaitDuration(Duration.ofMillis(200L));
        backendWithoutBaseConfig.setWritableStackTraceEnabled(true);
        assertThat(backendWithoutBaseConfig.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties.InstanceProperties backendWithSharedConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        backendWithSharedConfig.setBaseConfig("sharedConfig");
        backendWithSharedConfig.setMaxWaitDuration(Duration.ofMillis(300L));
        backendWithSharedConfig.setWritableStackTraceEnabled(false);
        assertThat(backendWithSharedConfig.getEventConsumerBufferSize()).isNull();

        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedProperties);

        bulkheadConfigurationProperties.getInstances()
            .put("backendWithoutBaseConfig", backendWithoutBaseConfig);
        bulkheadConfigurationProperties.getInstances()
            .put("backendWithSharedConfig", backendWithSharedConfig);

        //Then
        assertThat(bulkheadConfigurationProperties.getInstances()).hasSize(2);

        // Should get default config and overwrite max calls and wait time
        BulkheadConfig bulkhead1 = bulkheadConfigurationProperties
            .createBulkheadConfig(backendWithoutBaseConfig, compositeBulkheadCustomizer(),
                "backendWithoutBaseConfig");
        assertThat(bulkhead1).isNotNull();
        assertThat(bulkhead1.getMaxConcurrentCalls()).isEqualTo(3);
        assertThat(bulkhead1.getMaxWaitDuration().toMillis()).isEqualTo(200L);
        assertThat(bulkhead1.isWritableStackTraceEnabled()).isTrue();

        // Should get shared config and overwrite wait time
        BulkheadConfig bulkhead2 = bulkheadConfigurationProperties
            .createBulkheadConfig(backendWithSharedConfig, compositeBulkheadCustomizer(),
                "backendWithSharedConfig");
        assertThat(bulkhead2).isNotNull();
        assertThat(bulkhead2.getMaxConcurrentCalls()).isEqualTo(2);
        assertThat(bulkhead2.getMaxWaitDuration().toMillis()).isEqualTo(300L);
        assertThat(bulkhead2.isWritableStackTraceEnabled()).isFalse();

        // Unknown backend should get default config of Registry
        BulkheadConfig bulkhead3 = bulkheadConfigurationProperties
            .createBulkheadConfig(new CommonBulkheadConfigurationProperties.InstanceProperties(),
                compositeBulkheadCustomizer(), "unknown");
        assertThat(bulkhead3).isNotNull();
        assertThat(bulkhead3.getMaxWaitDuration().toMillis()).isEqualTo(50L);
        assertThat(bulkhead3.isWritableStackTraceEnabled()).isTrue();

    }

    @Test
    public void testCreateBulkHeadPropertiesWithUnknownConfig() {
        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();

        CommonBulkheadConfigurationProperties.InstanceProperties instanceProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
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
    public void testIllegalArgumentOnMaxConcurrentCallsLessThanOne() {
        CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxConcurrentCalls(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentOnMaxWaitDurationNegative() {
        CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setMaxWaitDuration(Duration.ofNanos(-1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBulkheadIllegalArgumentOnEventConsumerBufferSizeLessThanOne() {
        CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(0);
    }

    @Test
    public void testBulkheadConfigWithBaseConfig() {
        CommonBulkheadConfigurationProperties.InstanceProperties defaultConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultConfig.setMaxConcurrentCalls(2000);
        defaultConfig.setMaxWaitDuration(Duration.ofMillis(100L));

        CommonBulkheadConfigurationProperties.InstanceProperties sharedConfigWithDefaultConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        sharedConfigWithDefaultConfig.setMaxWaitDuration(Duration.ofMillis(1000L));
        sharedConfigWithDefaultConfig.setBaseConfig("defaultConfig");

        CommonBulkheadConfigurationProperties.InstanceProperties instanceWithSharedConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();
        instanceWithSharedConfig.setBaseConfig("sharedConfig");


        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("defaultConfig", defaultConfig);
        bulkheadConfigurationProperties.getConfigs().put("sharedConfig", sharedConfigWithDefaultConfig);
        bulkheadConfigurationProperties.getInstances().put("instanceWithSharedConfig", instanceWithSharedConfig);


        BulkheadConfig instance = bulkheadConfigurationProperties
            .createBulkheadConfig(instanceWithSharedConfig, compositeBulkheadCustomizer(), "instanceWithSharedConfig");
        assertThat(instance).isNotNull();
        assertThat(instance.getMaxConcurrentCalls()).isEqualTo(2000);
        assertThat(instance.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(1000L));
    }

    @Test
    public void testGetBackendPropertiesPropertiesWithoutDefaultConfig() {
        //Given
        CommonBulkheadConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();

        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getInstances().put("backendWithoutBaseConfig", backendWithoutBaseConfig);

        //Then
        assertThat(bulkheadConfigurationProperties.getInstances()).hasSize(1);

        // Should get defaults
        CommonBulkheadConfigurationProperties.InstanceProperties bulkheadProperties =
            bulkheadConfigurationProperties.getBackendProperties("backendWithoutBaseConfig");
        assertThat(bulkheadProperties).isNotNull();
        assertThat(bulkheadProperties.getEventConsumerBufferSize()).isNull();
    }

    @Test
    public void testGetBackendPropertiesPropertiesWithDefaultConfig() {
        //Given
        CommonBulkheadConfigurationProperties.InstanceProperties defaultProperties = new CommonBulkheadConfigurationProperties.InstanceProperties();
        defaultProperties.setEventConsumerBufferSize(99);

        CommonBulkheadConfigurationProperties.InstanceProperties backendWithoutBaseConfig = new CommonBulkheadConfigurationProperties.InstanceProperties();

        CommonBulkheadConfigurationProperties bulkheadConfigurationProperties = new CommonBulkheadConfigurationProperties();
        bulkheadConfigurationProperties.getConfigs().put("default", defaultProperties);
        bulkheadConfigurationProperties.getInstances().put("backendWithoutBaseConfig", backendWithoutBaseConfig);

        //Then
        assertThat(bulkheadConfigurationProperties.getInstances()).hasSize(1);

        // Should get default config and overwrite enableExponentialBackoff but not enableRandomizedWait
        CommonBulkheadConfigurationProperties.InstanceProperties bulkheadProperties =
            bulkheadConfigurationProperties.getBackendProperties("backendWithoutBaseConfig");
        assertThat(bulkheadProperties).isNotNull();
        assertThat(bulkheadProperties.getEventConsumerBufferSize()).isEqualTo(99);
    }

    private CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer() {
        return new CompositeCustomizer<>(Collections.emptyList());
    }
}
