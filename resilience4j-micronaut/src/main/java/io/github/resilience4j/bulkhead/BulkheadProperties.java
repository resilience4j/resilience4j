package io.github.resilience4j.bulkhead;

import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.core.util.Toggleable;

import java.util.Map;

@ConfigurationProperties("resilience4j.bulkhead")
public class BulkheadProperties extends BulkheadConfigurationProperties implements Toggleable {
    private boolean enabled;

    public void setConfigs(Map<String, BulkheadConfigurationProperties.InstanceProperties> configs) {
        this.getConfigs().putAll(configs);
    }

    public void setInstances(Map<String, BulkheadConfigurationProperties.InstanceProperties> instances) {
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
