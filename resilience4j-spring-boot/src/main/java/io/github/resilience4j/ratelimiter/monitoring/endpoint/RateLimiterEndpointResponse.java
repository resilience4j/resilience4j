package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import java.util.List;


public class RateLimiterEndpointResponse {

    private List<String> rateLimitersNames;

    public RateLimiterEndpointResponse(List<String> rateLimitersNames) {
        this.rateLimitersNames = rateLimitersNames;
    }

    public List<String> getRateLimitersNames() {
        return rateLimitersNames;
    }

    public void setRateLimitersNames(List<String> rateLimitersNames) {
        this.rateLimitersNames = rateLimitersNames;
    }
}
