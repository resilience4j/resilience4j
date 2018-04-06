package io.github.resilience4j.ratelimiter.autoconfigure;

import io.github.resilience4j.ratelimiter.configure.RateLimiterProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resilience4j.ratelimiter")
public class RateLimiterPropertiesAutoConfigured extends RateLimiterProperties {

}
