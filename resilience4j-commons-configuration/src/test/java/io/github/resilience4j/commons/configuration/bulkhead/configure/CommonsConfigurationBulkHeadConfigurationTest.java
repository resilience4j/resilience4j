/*
 *   Copyright 2026: Deepak Kumar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.github.resilience4j.commons.configuration.bulkhead.configure;

import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonsConfigurationBulkHeadConfigurationTest {
    @Test
    void fromPropertiesFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationBulkHeadConfiguration bulkHeadConfiguration  = CommonsConfigurationBulkHeadConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    @Test
    void fromYamlFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationBulkHeadConfiguration bulkHeadConfiguration  = CommonsConfigurationBulkHeadConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    private static void assertConfigs(Map<String, CommonBulkheadConfigurationProperties.InstanceProperties> config) {
        assertThat(config)
                .hasSize(1)
                .containsKey(TestConstants.DEFAULT);
        assertConfigDefault(config.get(TestConstants.DEFAULT));
    }

    private static void assertConfigDefault(CommonBulkheadConfigurationProperties.InstanceProperties configDefault) {
        assertThat(configDefault.getMaxWaitDuration()).isNull();
        assertThat(configDefault.getMaxConcurrentCalls()).isEqualTo(100);
        assertThat(configDefault.getEventConsumerBufferSize()).isNull();
        assertThat(configDefault.isWritableStackTraceEnabled()).isNull();
    }

    private static void assertInstances(Map<String, CommonBulkheadConfigurationProperties.InstanceProperties> instances) {
        assertThat(instances)
                .hasSize(2)
                .containsKey(TestConstants.BACKEND_A);
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private static void assertInstanceBackendA(CommonBulkheadConfigurationProperties.InstanceProperties instanceBackendA) {
        assertThat(instanceBackendA.getMaxWaitDuration()).isNull();
        assertThat(instanceBackendA.getMaxConcurrentCalls()).isEqualTo(10);
        assertThat(instanceBackendA.getEventConsumerBufferSize()).isNull();
        assertThat(instanceBackendA.isWritableStackTraceEnabled()).isNull();
    }

    private static void assertInstanceBackendB(CommonBulkheadConfigurationProperties.InstanceProperties instanceBackendB) {
        assertThat(instanceBackendB.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(10));
        assertThat(instanceBackendB.getMaxConcurrentCalls()).isEqualTo(20);
        assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(15);
        assertThat(instanceBackendB.isWritableStackTraceEnabled()).isTrue();
    }
}