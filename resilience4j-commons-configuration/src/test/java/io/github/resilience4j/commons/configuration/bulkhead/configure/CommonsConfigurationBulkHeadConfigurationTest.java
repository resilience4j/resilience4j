package io.github.resilience4j.commons.configuration.bulkhead.configure;

import io.github.resilience4j.common.bulkhead.configuration.CommonBulkheadConfigurationProperties;
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

public class CommonsConfigurationBulkHeadConfigurationTest {
    @Test
    public void testFromPropertiesFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationBulkHeadConfiguration bulkHeadConfiguration  = CommonsConfigurationBulkHeadConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    @Test
    public void testFromYamlFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationBulkHeadConfiguration bulkHeadConfiguration  = CommonsConfigurationBulkHeadConfiguration.of(config);

        assertConfigs(bulkHeadConfiguration.getConfigs());
        assertInstances(bulkHeadConfiguration.getInstances());
    }

    private static void assertConfigs(Map<String, CommonBulkheadConfigurationProperties.InstanceProperties> config) {
        Assertions.assertThat(config.size()).isEqualTo(1);
        Assertions.assertThat(config.containsKey(TestConstants.DEFAULT)).isTrue();
        assertConfigDefault(config.get(TestConstants.DEFAULT));
    }

    private static void assertConfigDefault(CommonBulkheadConfigurationProperties.InstanceProperties configDefault) {
        Assertions.assertThat(configDefault.getMaxWaitDuration()).isNull();
        Assertions.assertThat(configDefault.getMaxConcurrentCalls()).isEqualTo(100);
        Assertions.assertThat(configDefault.getEventConsumerBufferSize()).isNull();
        Assertions.assertThat(configDefault.isWritableStackTraceEnabled()).isNull();
    }

    private static void assertInstances(Map<String, CommonBulkheadConfigurationProperties.InstanceProperties> instances) {
        Assertions.assertThat(instances.size()).isEqualTo(2);
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_A)).isTrue();
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private static void assertInstanceBackendA(CommonBulkheadConfigurationProperties.InstanceProperties instanceBackendA) {
        Assertions.assertThat(instanceBackendA.getMaxWaitDuration()).isNull();
        Assertions.assertThat(instanceBackendA.getMaxConcurrentCalls()).isEqualTo(10);
        Assertions.assertThat(instanceBackendA.getEventConsumerBufferSize()).isNull();
        Assertions.assertThat(instanceBackendA.isWritableStackTraceEnabled()).isNull();
    }

    private static void assertInstanceBackendB(CommonBulkheadConfigurationProperties.InstanceProperties instanceBackendB) {
        Assertions.assertThat(instanceBackendB.getMaxWaitDuration()).isEqualTo(Duration.ofMillis(10));
        Assertions.assertThat(instanceBackendB.getMaxConcurrentCalls()).isEqualTo(20);
        Assertions.assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(15);
        Assertions.assertThat(instanceBackendB.isWritableStackTraceEnabled()).isTrue();
    }
}