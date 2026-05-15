package io.github.resilience4j.springboot.circuitbreaker;

import java.util.Map;

public final class CompositeHealthResponse {

    private String status;
    private Map<String, HealthResponse> details;

    public Map<String, HealthResponse> getDetails() {
        return details;
    }

    void setDetails(Map<String, HealthResponse> details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }
}
