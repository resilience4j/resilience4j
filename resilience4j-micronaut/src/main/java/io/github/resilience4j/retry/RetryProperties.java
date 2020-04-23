package io.github.resilience4j.retry;


import io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Map;

@ConfigurationProperties("resilience4j.retry")
public class RetryProperties extends RetryConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, RetryConfigurationProperties.InstanceProperties> configs) {
        this.getConfigs().putAll(configs);
    }

    public void setInstances(Map<String, RetryConfigurationProperties.InstanceProperties> instances) {
        this.getInstances().putAll(instances);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
