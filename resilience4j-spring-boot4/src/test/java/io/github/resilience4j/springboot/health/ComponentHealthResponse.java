package io.github.resilience4j.springboot.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * Test helper mirroring a single component of the composite {@code /actuator/health} endpoint,
 * e.g. the {@code circuitBreakers} or {@code rateLimiters} entry, whose {@code details} map is keyed
 * by backend name.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentHealthResponse {

    private String status;
    private Map<String, Object> details;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
