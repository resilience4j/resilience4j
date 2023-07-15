/*
 *   Copyright 2023: Deepak Kumar
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
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

public class CommonsConfigurationTimeLimiterConfigurationTest {
    @Test
    public void testFromPropertiesFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationTimeLimiterConfiguration timeLimiterConfiguration  = CommonsConfigurationTimeLimiterConfiguration.of(config);

        assertConfigs(timeLimiterConfiguration.getConfigs());
        assertInstances(timeLimiterConfiguration.getInstances());
    }

    @Test
    public void testFromYamlFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationTimeLimiterConfiguration timeLimiterConfiguration  = CommonsConfigurationTimeLimiterConfiguration.of(config);

        assertConfigs(timeLimiterConfiguration.getConfigs());
        assertInstances(timeLimiterConfiguration.getInstances());
    }

    private void assertConfigs(Map<String, CommonTimeLimiterConfigurationProperties.InstanceProperties> config) {
        Assertions.assertThat(config.size()).isEqualTo(1);
        Assertions.assertThat(config.containsKey(TestConstants.DEFAULT)).isTrue();
        assertConfigDefault(config.get(TestConstants.DEFAULT));
    }

    private void assertConfigDefault(CommonTimeLimiterConfigurationProperties.InstanceProperties configDefault) {
        Assertions.assertThat(configDefault.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(configDefault.getCancelRunningFuture()).isTrue();
        Assertions.assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(100);
    }

    private void assertInstances(Map<String, CommonTimeLimiterConfigurationProperties.InstanceProperties> instances) {
        Assertions.assertThat(instances.size()).isEqualTo(2);
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_A)).isTrue();
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_B)).isTrue();
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private void assertInstanceBackendA(CommonTimeLimiterConfigurationProperties.InstanceProperties instanceBackendA) {
        Assertions.assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        Assertions.assertThat(instanceBackendA.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
        Assertions.assertThat(instanceBackendA.getCancelRunningFuture()).isNull();
        Assertions.assertThat(instanceBackendA.getEventConsumerBufferSize()).isNull();
    }

    private void assertInstanceBackendB(CommonTimeLimiterConfigurationProperties.InstanceProperties instanceBackendB) {
        Assertions.assertThat(instanceBackendB.getBaseConfig()).isNull();
        Assertions.assertThat(instanceBackendB.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(15));
        Assertions.assertThat(instanceBackendB.getCancelRunningFuture()).isFalse();
        Assertions.assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(10);
    }
}