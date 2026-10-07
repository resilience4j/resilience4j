package io.github.resilience4j.springboot.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Test helper mirroring the JSON returned by the composite {@code /actuator/health} endpoint,
 * exposing the top-level {@code components} map (e.g. {@code circuitBreakers}, {@code rateLimiters}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RootHealthResponse {

    private String status;
    private Map<String, ComponentHealthResponse> components;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, ComponentHealthResponse> getComponents() {
        return components;
    }

    public void setComponents(Map<String, ComponentHealthResponse> components) {
        this.components = components;
    }
}
