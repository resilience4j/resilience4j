package io.github.resilience4j.common.retry.configuration;

import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.retry.RetryConfig;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class CommonRetryConfigurationPropertiesTest {

    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_WAIT_DURATION = instanceProperties -> instanceProperties.setWaitDuration(Duration.ofSeconds(1));
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> ENABLE_EXPONENTIAL_BACKOFF = instanceProperties -> instanceProperties.setEnableExponentialBackoff(true);
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_BACKOFF_MULTIPLIER = instanceProperties -> instanceProperties.setExponentialBackoffMultiplier(2.0);
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_EXPONENTIAL_MAX_WAIT_DURATION = instanceProperties -> instanceProperties.setExponentialMaxWaitDuration(Duration.ofSeconds(5));
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> ENABLE_RANDOMIZED_WAIT = instanceProperties -> instanceProperties.setEnableRandomizedWait(true);
    private static final Consumer<CommonRetryConfigurationProperties.InstanceProperties> WITH_RANDOMIZED_FACTOR = instanceProperties -> instanceProperties.setRandomizedWaitFactor(0.5);

    @Test
    public void createRetryConfig_withDefault() {
        testCreateRetryConfig();
    }

    @Test
    public void createRetryConfig_withWaitDuration() {
        testCreateRetryConfig(WITH_WAIT_DURATION);
    }

    @Test
    public void createRetryConfig_withExponentialBackoff() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_EXPONENTIAL_BACKOFF);
    }

    @Test
    public void createRetryConfig_withBackoffMultiplier() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER);
    }

    @Test
    public void createRetryConfig_withExponentialMaxWaitDuration() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER, WITH_EXPONENTIAL_MAX_WAIT_DURATION);
    }

    @Test
    public void createRetryConfig_withRandomizedWait() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT);
    }

    @Test
    public void createRetryConfig_withRandomizedFactor() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, WITH_RANDOMIZED_FACTOR);
    }

    @Test
    public void createRetryConfig_withRandomizedExponentialBackoff() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, ENABLE_EXPONENTIAL_BACKOFF);
    }

    @Test
    public void createRetryConfig_withRandomizedExponentialBackoffMultiplier() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER);
    }

    @Test
    public void createRetryConfig_withRandomizedFactorExponentialBackoffMultiplier() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, WITH_RANDOMIZED_FACTOR, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER);
    }

    @Test
    public void createRetryConfig_withRandomizedExponentialMaxWaitDuration() {
        testCreateRetryConfig(WITH_WAIT_DURATION, ENABLE_RANDOMIZED_WAIT, WITH_RANDOMIZED_FACTOR, ENABLE_EXPONENTIAL_BACKOFF, WITH_BACKOFF_MULTIPLIER, WITH_EXPONENTIAL_MAX_WAIT_DURATION);
    }

    @SafeVarargs
    private void testCreateRetryConfig(Consumer<CommonRetryConfigurationProperties.InstanceProperties>... customizers) {
        String defaultConfigurationName = "default";
        String testConfigurationName = "test";

        CommonRetryConfigurationProperties properties = new CommonRetryConfigurationProperties();
        properties.getConfigs().put(defaultConfigurationName, new CommonRetryConfigurationProperties.InstanceProperties().setWaitDuration(Duration.ofSeconds(1)));

        CommonRetryConfigurationProperties.InstanceProperties instanceProperties = new CommonRetryConfigurationProperties.InstanceProperties().setBaseConfig(defaultConfigurationName);
        Arrays.stream(customizers).forEachOrdered(customizer -> customizer.accept(instanceProperties));
        properties.getInstances().put(testConfigurationName, instanceProperties);

        RetryConfig retryConfig = properties.createRetryConfig(testConfigurationName, new CompositeCustomizer<>(List.of()));

        Assertions.assertThat(retryConfig).isNotNull(); // assert that retryConfig does not throw an exception
        Assertions.assertThat(retryConfig.getIntervalFunction()).isNull();
        Assertions.assertThat(retryConfig.getIntervalBiFunction()).isNotNull();
    }

}