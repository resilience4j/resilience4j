package io.github.resilience4j.commons.configuration.circuitbreaker.configure;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CommonCircuitBreakerConfigurationProperties;
import io.github.resilience4j.commons.configuration.dummy.DummyIgnoredException;
import io.github.resilience4j.commons.configuration.dummy.DummyRecordFailurePredicate;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

public class CommonsConfigurationCircuitBreakerConfigurationTest {

    static final String RESILIENCE_CONFIG_PROPERTIES_FILE_NAME = "resilience.properties";
    static final String BACKEND_A = "backendA";
    static final String BACKEND_B = "backendB";
    private static final String DEFAULT = "default";
    private static final String SHARED = "shared";

    @Test
    public void testFromPropertiesFile() throws ConfigurationException {
        FileBasedConfigurationBuilder<PropertiesConfiguration> builder = new FileBasedConfigurationBuilder<>(PropertiesConfiguration.class)
                .configure(new Parameters()
                                .fileBased()
                                .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                                .setFileName(RESILIENCE_CONFIG_PROPERTIES_FILE_NAME));
        Configuration config = builder.getConfiguration();

        CommonsConfigurationCircuitBreakerConfiguration commonsConfigurationCircuitBreakerConfiguration = CommonsConfigurationCircuitBreakerConfiguration.of(config);

        assertConfigs(commonsConfigurationCircuitBreakerConfiguration.getConfigs());
        assertInstances(commonsConfigurationCircuitBreakerConfiguration.getInstances());
    }

    private static void assertConfigs(Map<String, CommonCircuitBreakerConfigurationProperties.InstanceProperties> config) {
        Assertions.assertThat(config.size()).isEqualTo(2);
        Assertions.assertThat(config.containsKey(DEFAULT)).isTrue();
        assertConfigDefault(config.get(DEFAULT));
        Assertions.assertThat(config.containsKey(SHARED)).isTrue();
        assertConfigShared(config.get(SHARED));
    }

    private static void assertConfigDefault(CommonCircuitBreakerConfigurationProperties.InstanceProperties configDefault) {
        Assertions.assertThat(configDefault.getSlidingWindowSize()).isEqualTo(100);
        Assertions.assertThat(configDefault.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(10);
        Assertions.assertThat(configDefault.getWaitDurationInOpenState()).isEqualTo(Duration.ofMinutes(1));
        Assertions.assertThat(configDefault.getFailureRateThreshold()).isEqualTo(50f);
        Assertions.assertThat(configDefault.getSlowCallRateThreshold()).isEqualTo(50f);
        Assertions.assertThat(configDefault.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
        Assertions.assertThat(configDefault.getWritableStackTraceEnabled()).isEqualTo(true);
        Assertions.assertThat(configDefault.getAutomaticTransitionFromOpenToHalfOpenEnabled()).isEqualTo(true);
        Assertions.assertThat(configDefault.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
        Assertions.assertThat(configDefault.getMinimumNumberOfCalls()).isEqualTo(5);
        Assertions.assertThat(configDefault.getEventConsumerBufferSize()).isEqualTo(10);
        Assertions.assertThat(configDefault.getRegisterHealthIndicator()).isEqualTo(true);
        Assertions.assertThat(configDefault.getRecordFailurePredicate()).isEqualTo(DummyRecordFailurePredicate.class);
        Assertions.assertThat(configDefault.getRecordExceptions()).containsExactly(IllegalArgumentException.class, NullPointerException.class);
        Assertions.assertThat(configDefault.getIgnoreExceptionPredicate()).isEqualTo(DummyRecordFailurePredicate.class);
        Assertions.assertThat(configDefault.getIgnoreExceptions()).containsExactly(IllegalStateException.class, IndexOutOfBoundsException.class);
    }

    private static void assertConfigShared(CommonCircuitBreakerConfigurationProperties.InstanceProperties configShared) {
        Assertions.assertThat(configShared.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
        Assertions.assertThat(configShared.getSlidingWindowSize()).isEqualTo(100);
        Assertions.assertThat(configShared.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(30);
        Assertions.assertThat(configShared.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(10));
        Assertions.assertThat(configShared.getFailureRateThreshold()).isEqualTo(50f);
        Assertions.assertThat(configShared.getEventConsumerBufferSize()).isEqualTo(10);
        Assertions.assertThat(configShared.getIgnoreExceptions()).containsExactly(DummyIgnoredException.class);
    }

    private static void assertInstances(Map<String, CommonCircuitBreakerConfigurationProperties.InstanceProperties> instances) {
        Assertions.assertThat(instances.size()).isEqualTo(2);
        Assertions.assertThat(instances.containsKey(BACKEND_A)).isTrue();
        assertInstanceBackendA(instances.get(BACKEND_A));
        assertInstanceBackendB(instances.get(BACKEND_B));
    }

    private static void assertInstanceBackendA(CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceBackendA) {
        Assertions.assertThat(instanceBackendA.getBaseConfig()).isEqualTo(DEFAULT);
        Assertions.assertThat(instanceBackendA.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(5));
    }

    private static void assertInstanceBackendB(CommonCircuitBreakerConfigurationProperties.InstanceProperties instanceBackendB) {
        Assertions.assertThat(instanceBackendB.getBaseConfig()).isEqualTo(SHARED);
        Assertions.assertThat(instanceBackendB.getRegisterHealthIndicator()).isEqualTo(true);
        Assertions.assertThat(instanceBackendB.getSlidingWindowSize()).isEqualTo(10);
        Assertions.assertThat(instanceBackendB.getMinimumNumberOfCalls()).isEqualTo(10);
        Assertions.assertThat(instanceBackendB.getFailureRateThreshold()).isEqualTo(60f);
        Assertions.assertThat(instanceBackendB.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
        Assertions.assertThat(instanceBackendB.getWaitDurationInOpenState()).isEqualTo(Duration.ofSeconds(5));
        Assertions.assertThat(instanceBackendB.getEventConsumerBufferSize()).isEqualTo(20);
        Assertions.assertThat(instanceBackendB.getRecordFailurePredicate()).isEqualTo(DummyRecordFailurePredicate.class);
    }
}