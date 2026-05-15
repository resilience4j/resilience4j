package io.github.resilience4j.springboot.circuitbreaker;

import java.util.Map;

public class HealthResponse {

    private String status;

    private Map<String, Object> details;

    public Map<String, Object> getDetails() {
        return details;
    }

    void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    void setStatus(String status) {
        this.status = status;
    }
}
