package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Map;

@ConfigurationProperties("resilience4j.circuitbreaker")
public class CircuitBreakerProperties extends CircuitBreakerConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, CircuitBreakerConfigurationProperties.InstanceProperties> configs){
        this.getConfigs().putAll(configs);
    }

    public void setInstances(Map<String, CircuitBreakerConfigurationProperties.InstanceProperties> instances) {
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
