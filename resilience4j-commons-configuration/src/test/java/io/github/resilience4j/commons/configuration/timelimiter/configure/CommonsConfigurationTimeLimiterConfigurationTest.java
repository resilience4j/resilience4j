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

package io.github.resilience4j.commons.configuration.timelimiter.configure;

import io.github.resilience4j.common.timelimiter.configuration.CommonTimeLimiterConfigurationProperties;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonsConfigurationTimeLimiterConfigurationTest {
    @Test
    void fromPropertiesFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationTimeLimiterConfiguration timeLimiterConfiguration  = CommonsConfigurationTimeLimiterConfiguration.of(config);

        assertConfigs(timeLimiterConfiguration.getConfigs());
        assertInstances(timeLimiterConfiguration.getInstances());
    }

    @Test
    void fromYamlFile() throws Exception {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationTimeLimiterConfiguration timeLimiterConfiguration  = CommonsConfigurationTimeLimiterConfiguration.of(config);

        assertConfigs(timeLimiterConfiguration.getConfigs());
        assertInstances(timeLimiterConfiguration.getInstances());
    }

    private void assertConfigs(Map<String, CommonTimeLimiterConfigurationProperties.InstanceProperties> config) {
        assertThat(config)
                .hasSize(1)
                .containsKey(TestConstants.DEFAULT);
        assertConfigDefault(config.get(TestConstants.DEFAULT));
    }

    private void assertConfigDefault(CommonTimeLimiterConfigurationProperties.InstanceProperties configDefault) {
        assertThat(configDefault.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(10));
        assertThat(configDefault.getCancelRunningFuture()).isTrue();
        assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(100);
    }

    private void assertInstances(Map<String, CommonTimeLimiterConfigurationProperties.InstanceProperties> instances) {
        assertThat(instances)
                .hasSize(2)
                .containsKey(TestConstants.BACKEND_A)
                .containsKey(TestConstants.BACKEND_B);
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private void assertInstanceBackendA(CommonTimeLimiterConfigurationProperties.InstanceProperties instanceBackendA) {
        assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        assertThat(instanceBackendA.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
        assertThat(instanceBackendA.getCancelRunningFuture()).isNull();
        assertThat(instanceBackendA.getEventConsumerBufferSize()).isNull();
    }

    private void assertInstanceBackendB(CommonTimeLimiterConfigurationProperties.InstanceProperties instanceBackendB) {
        assertThat(instanceBackendB.getBaseConfig()).isNull();
        assertThat(instanceBackendB.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(15));
        assertThat(instanceBackendB.getCancelRunningFuture()).isFalse();
        assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(10);
    }
}