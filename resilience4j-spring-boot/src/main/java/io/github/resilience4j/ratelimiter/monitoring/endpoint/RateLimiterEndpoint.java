package io.github.resilience4j.ratelimiter.monitoring.endpoint;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * {@link Endpoint} to expose CircuitBreaker events.
 */
@ConfigurationProperties(prefix = "endpoints.ratelimiter")
public class RateLimiterEndpoint extends AbstractEndpoint {

    private final RateLimiterRegistry rateLimiterRegistry;

    public RateLimiterEndpoint(RateLimiterRegistry rateLimiterRegistry) {
        super("ratelimiter");
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Override
    public Object invoke() {
        List<String> names = rateLimiterRegistry.getAllRateLimiters()
            .map(RateLimiter::getName).sorted().toJavaList();
        return ResponseEntity.ok(new RateLimiterEndpointResponse(names));
    }
}
