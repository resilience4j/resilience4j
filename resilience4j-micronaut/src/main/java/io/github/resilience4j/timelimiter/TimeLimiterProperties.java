package io.github.resilience4j.timelimiter;

import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@ConfigurationProperties("resilience4j.timelimiter")
public class TimeLimiterProperties extends TimeLimiterConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, TimeLimiterConfigurationProperties.InstanceProperties> configs) {
        this.getConfigs().putAll(configs);
    }

    public void setInstances(Map<String, TimeLimiterConfigurationProperties.InstanceProperties> instances) {
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
