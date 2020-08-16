package io.github.resilience4j.ratelimiter.monitoring.health;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.configure.RateLimiterConfigurationProperties;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bstorozhuk
 */
public class RateLimitersHealthIndicatorTest {

    @Test
    public void health() throws Exception {
        // given
        RateLimiterConfig config = mock(RateLimiterConfig.class);
        AtomicRateLimiter.AtomicRateLimiterMetrics metrics = mock(
            AtomicRateLimiter.AtomicRateLimiterMetrics.class);
        AtomicRateLimiter rateLimiter = mock(AtomicRateLimiter.class);
        RateLimiterRegistry rateLimiterRegistry = mock(RateLimiterRegistry.class);
        io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties instanceProperties =
            mock(
                io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties.class);
        RateLimiterConfigurationProperties rateLimiterProperties = mock(
            RateLimiterConfigurationProperties.class);

        //when
        when(rateLimiter.getRateLimiterConfig()).thenReturn(config);
        when(rateLimiter.getName()).thenReturn("test");
        when(rateLimiterProperties.findRateLimiterProperties("test"))
            .thenReturn(Optional.of(instanceProperties));
        when(instanceProperties.getRegisterHealthIndicator()).thenReturn(true);
        when(instanceProperties.getAllowHealthIndicatorToFail()).thenReturn(true);
        when(rateLimiter.getMetrics()).thenReturn(metrics);
        when(rateLimiter.getDetailedMetrics()).thenReturn(metrics);
        when(rateLimiterRegistry.getAllRateLimiters()).thenReturn(Set.of(rateLimiter));

        when(config.getTimeoutDuration()).thenReturn(Duration.ofNanos(30L));

        when(metrics.getAvailablePermissions())
                .thenReturn(5, -1, -2);
        when(metrics.getNumberOfWaitingThreads())
                .thenReturn(0, 1, 2);
        when(metrics.getNanosToWait())
                .thenReturn(20L, 40L);

        // then
        RateLimitersHealthIndicator healthIndicator =
            new RateLimitersHealthIndicator(rateLimiterRegistry, rateLimiterProperties, new SimpleStatusAggregator());

        Health health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UP);

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UNKNOWN);

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.DOWN);

        then(health.getDetails().get("test")).isInstanceOf(Health.class);
        then(((Health) health.getDetails().get("test")).getDetails())
                .contains(
                        entry("availablePermissions", -2),
                        entry("numberOfWaitingThreads", 2)
                );
    }

    @Test
    public void healthIndicatorMaxImpactCanBeOverridden() throws Exception {
        // given
        RateLimiterConfig config = mock(RateLimiterConfig.class);
        AtomicRateLimiter.AtomicRateLimiterMetrics metrics = mock(AtomicRateLimiter.AtomicRateLimiterMetrics.class);
        AtomicRateLimiter rateLimiter = mock(AtomicRateLimiter.class);
        RateLimiterRegistry rateLimiterRegistry = mock(RateLimiterRegistry.class);
        io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties instanceProperties =
                mock(io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties.InstanceProperties.class);
        RateLimiterConfigurationProperties rateLimiterProperties = mock(RateLimiterConfigurationProperties.class);

        //when
        when(rateLimiter.getRateLimiterConfig()).thenReturn(config);
        when(rateLimiter.getName()).thenReturn("test");
        when(rateLimiterProperties.findRateLimiterProperties("test")).thenReturn(Optional.of(instanceProperties));
        when(instanceProperties.getRegisterHealthIndicator()).thenReturn(true);

        boolean allowHealthIndicatorToFail = false; // do not allow health indicator to fail
        when(instanceProperties.getAllowHealthIndicatorToFail()).thenReturn(allowHealthIndicatorToFail);
        when(rateLimiter.getMetrics()).thenReturn(metrics);
        when(rateLimiter.getDetailedMetrics()).thenReturn(metrics);
        when(rateLimiterRegistry.getAllRateLimiters()).thenReturn(Set.of(rateLimiter));

        when(config.getTimeoutDuration()).thenReturn(Duration.ofNanos(30L));

        when(metrics.getAvailablePermissions())
                .thenReturn(-2);
        when(metrics.getNumberOfWaitingThreads())
                .thenReturn(2);
        when(metrics.getNanosToWait())
                .thenReturn(40L);

        // then
        RateLimitersHealthIndicator healthIndicator =
                new RateLimitersHealthIndicator(rateLimiterRegistry, rateLimiterProperties, new SimpleStatusAggregator());

        Health health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UNKNOWN);
        then(((Health) health.getDetails().get("test")).getStatus()).isEqualTo(new Status("RATE_LIMITED"));

        then(health.getDetails().get("test")).isInstanceOf(Health.class);
        then(((Health) health.getDetails().get("test")).getDetails())
                .contains(
                        entry("availablePermissions", -2),
                        entry("numberOfWaitingThreads", 2)
                );
    }

    private SimpleEntry<String, ?> entry(String key, Object value) {
        return new SimpleEntry<>(key, value);
    }
}
