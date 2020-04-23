package io.github.resilience4j.ratelimiter;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.web.router.RouteMatch;

import javax.inject.Singleton;
import java.util.Optional;

@Singleton
public class RateLimiterSupport {
    private final RateLimiterProperties configuration;

    /**
     * Default constructor.
     *
     * @param configuration The rate limiting configuration
     */
    public RateLimiterSupport(RateLimiterProperties configuration) {
        this.configuration = configuration;
    }

    /**
     * @param request The request
     * @return True if the request should be rate limited
     */
    public boolean shouldLimit(HttpRequest<?> request) {
        return getAnnotationMetadata(request).map(metadata -> metadata.hasAnnotation(RateLimiter.class)).orElse(true);
    }

    /**
     * @param request The request
     * @return The rate limiting configuration to be used for this request. Returns
     * empty if the default configuration should apply
     */
    public Optional<String> getConfigurationName(HttpRequest<?> request) {
        return getAnnotationMetadata(request).flatMap(metadata -> metadata.stringValue(RateLimiter.class));
    }

    /**
     * @param request The request
     * @return The annotation metadata to resolve rate limit annotations
     */
    protected Optional<AnnotationMetadata> getAnnotationMetadata(HttpRequest<?> request) {
        return request.getAttribute(HttpAttributes.ROUTE_MATCH, RouteMatch.class)
            .map(RouteMatch::getAnnotationMetadata);
    }
}
