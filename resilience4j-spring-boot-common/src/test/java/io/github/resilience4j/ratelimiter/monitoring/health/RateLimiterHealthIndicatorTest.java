package io.github.resilience4j.ratelimiter.monitoring.health;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.internal.AtomicRateLimiter;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author bstorozhuk
 */
public class RateLimiterHealthIndicatorTest {
    @Test
    public void health() throws Exception {
        // given
        RateLimiterConfig config = mock(RateLimiterConfig.class);
        AtomicRateLimiter.AtomicRateLimiterMetrics metrics = mock(AtomicRateLimiter.AtomicRateLimiterMetrics.class);
        AtomicRateLimiter rateLimiter = mock(AtomicRateLimiter.class);

        //when

        when(rateLimiter.getRateLimiterConfig()).thenReturn(config);
        when(rateLimiter.getMetrics()).thenReturn(metrics);
        when(rateLimiter.getDetailedMetrics()).thenReturn(metrics);

        when(config.getTimeoutDuration()).thenReturn(Duration.ofNanos(30L));

        when(metrics.getAvailablePermissions())
            .thenReturn(5, -1, -2);
        when(metrics.getNumberOfWaitingThreads())
            .thenReturn(0, 1, 2);
        when(metrics.getNanosToWait())
            .thenReturn(20L, 40L);

        // then
        RateLimiterHealthIndicator healthIndicator = new RateLimiterHealthIndicator(rateLimiter);

        Health health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UP);

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.UNKNOWN);

        health = healthIndicator.health();
        then(health.getStatus()).isEqualTo(Status.DOWN);

        then(health.getDetails())
            .contains(
                entry("availablePermissions", -2),
                entry("numberOfWaitingThreads", 2)
            );


    }

    private SimpleEntry<String, ?> entry(String key, Object value) {
        return new SimpleEntry<>(key, value);
    }
}