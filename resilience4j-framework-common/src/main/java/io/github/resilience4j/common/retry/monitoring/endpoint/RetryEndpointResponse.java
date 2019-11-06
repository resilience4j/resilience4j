package io.github.resilience4j.common.retry.monitoring.endpoint;

import java.util.List;

/**
 * retry get all retries response DTO
 */
public class RetryEndpointResponse {

    private List<String> retries;

    public RetryEndpointResponse() {
    }

    public RetryEndpointResponse(List<String> retries) {
        this.retries = retries;
    }

    public List<String> getRetries() {
        return retries;
    }

    public void setRetries(List<String> retries) {
        this.retries = retries;
    }
}
