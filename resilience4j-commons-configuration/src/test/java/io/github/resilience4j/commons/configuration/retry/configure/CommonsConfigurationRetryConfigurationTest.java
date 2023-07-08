package io.github.resilience4j.commons.configuration.retry.configure;

import io.github.resilience4j.common.retry.configuration.CommonRetryConfigurationProperties;
import io.github.resilience4j.commons.configuration.dummy.DummyIgnoredException;
import io.github.resilience4j.commons.configuration.dummy.DummyPredicateObject;
import io.github.resilience4j.commons.configuration.dummy.DummyPredicateThrowable;
import io.github.resilience4j.commons.configuration.util.CommonsConfigurationUtil;
import io.github.resilience4j.commons.configuration.util.TestConstants;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class CommonsConfigurationRetryConfigurationTest {

    @Test
    public void testFromPropertiesFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(PropertiesConfiguration.class, TestConstants.RESILIENCE_CONFIG_PROPERTIES_FILE_NAME);

        CommonsConfigurationRetryConfiguration retryConfiguration = CommonsConfigurationRetryConfiguration.of(config);

        assertConfigs(retryConfiguration.getConfigs());
        assertInstances(retryConfiguration.getInstances());
    }

    @Test
    public void testFromYamlFile() throws ConfigurationException {
        Configuration config = CommonsConfigurationUtil.getConfiguration(YAMLConfiguration.class, TestConstants.RESILIENCE_CONFIG_YAML_FILE_NAME);

        CommonsConfigurationRetryConfiguration retryConfiguration = CommonsConfigurationRetryConfiguration.of(config);

        assertConfigs(retryConfiguration.getConfigs());
        assertInstances(retryConfiguration.getInstances());
    }

    private void assertConfigs(Map<String, CommonRetryConfigurationProperties.InstanceProperties> configs) {
        Assertions.assertThat(configs.size()).isEqualTo(1);
        Assertions.assertThat(configs.containsKey(TestConstants.DEFAULT)).isTrue();
        assertConfigDefault(configs.get(TestConstants.DEFAULT));
    }

    private void assertConfigDefault(CommonRetryConfigurationProperties.InstanceProperties instanceProperties) {
        Assertions.assertThat(instanceProperties.getMaxAttempts()).isEqualTo(3);
        Assertions.assertThat(instanceProperties.getWaitDuration()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(instanceProperties.getRetryExceptions()).containsExactlyInAnyOrder(TimeoutException.class,
                IOException.class);
        Assertions.assertThat(instanceProperties.getIgnoreExceptions()).containsExactlyInAnyOrder(DummyIgnoredException.class, RuntimeException.class);
    }

    private void assertInstances(Map<String, CommonRetryConfigurationProperties.InstanceProperties> instances) {
        Assertions.assertThat(instances.size()).isEqualTo(2);
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_A)).isTrue();
        Assertions.assertThat(instances.containsKey(TestConstants.BACKEND_B)).isTrue();
        assertInstanceBackendA(instances.get(TestConstants.BACKEND_A));
        assertInstanceBackendB(instances.get(TestConstants.BACKEND_B));
    }

    private void assertInstanceBackendA(CommonRetryConfigurationProperties.InstanceProperties instanceBackendA) {
        Assertions.assertThat(instanceBackendA.getBaseConfig()).isEqualTo(TestConstants.DEFAULT);
        Assertions.assertThat(instanceBackendA.getMaxAttempts()).isNull();
        Assertions.assertThat(instanceBackendA.getWaitDuration()).isNull();
        Assertions.assertThat(instanceBackendA.getIgnoreExceptions()).isNull();
        Assertions.assertThat(instanceBackendA.getRetryExceptions()).isNull();
        ;
    }

    private void assertInstanceBackendB(CommonRetryConfigurationProperties.InstanceProperties instanceBackendB) {
        Assertions.assertThat(instanceBackendB.getBaseConfig()).isNull();
        Assertions.assertThat(instanceBackendB.getMaxAttempts()).isEqualTo(5);
        Assertions.assertThat(instanceBackendB.getRetryExceptionPredicate()).isEqualTo(DummyPredicateThrowable.class);
        Assertions.assertThat(instanceBackendB.getResultPredicate()).isEqualTo(DummyPredicateObject.class);
        Assertions.assertThat(instanceBackendB.getRetryExceptions()).containsExactlyInAnyOrder(TimeoutException.class, IOException.class);
        Assertions.assertThat(instanceBackendB.getIgnoreExceptions()).containsExactlyInAnyOrder(DummyIgnoredException.class, RuntimeException.class);
        Assertions.assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(10);
        Assertions.assertThat(instanceBackendB.getEnableExponentialBackoff()).isTrue();
        Assertions.assertThat(instanceBackendB.getExponentialBackoffMultiplier()).isEqualTo(2);
        Assertions.assertThat(instanceBackendB.getExponentialMaxWaitDuration()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(instanceBackendB.getEnableRandomizedWait()).isFalse();
        Assertions.assertThat(instanceBackendB.getFailAfterMaxAttempts()).isTrue();
    }


}