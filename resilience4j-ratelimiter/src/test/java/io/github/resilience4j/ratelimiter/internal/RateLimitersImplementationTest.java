package io.github.resilience4j.ratelimiter.internal;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.ceil;
import static java.util.Collections.synchronizedList;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for functions that are common in all implementations should go here
 */
public abstract class RateLimitersImplementationTest {

    protected abstract RateLimiter buildRateLimiter(RateLimiterConfig config);

    @Test
    public void aquireBigNumberOfPermitsAtStartOfCycleTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(5);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(1);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInNewCyclePermission = limiter.acquirePermission(1);
        then(retryInNewCyclePermission).isTrue();
    }

    @Test
    public void tryAquiringBigNumberOfPermitsAtEndOfCycleTest() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofNanos(250_000_000L))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();

        waitForRefresh(metrics, config, '.');

        boolean firstPermission = limiter.acquirePermission(1);
        then(firstPermission).isTrue();
        boolean secondPermission = limiter.acquirePermission(5);
        then(secondPermission).isTrue();
        boolean firstNoPermission = limiter.acquirePermission(5);
        then(firstNoPermission).isFalse();

        waitForRefresh(metrics, config, '*');

        boolean retryInSecondCyclePermission = limiter.acquirePermission(5);
        then(retryInSecondCyclePermission).isTrue();
    }

    @Test
    public void reservePermissionsUpfront() throws InterruptedException {
        final int limitForPeriod = 3;
        final int tasksNum = 9;
        Duration limitRefreshPeriod = Duration.ofMillis(1000);
        Duration timeoutDuration = Duration.ofMillis(1200);

        Duration durationToWait = limitRefreshPeriod.multipliedBy((long) ceil(((double) tasksNum) / limitForPeriod));

        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(limitForPeriod)
            .limitRefreshPeriod(limitRefreshPeriod)
            .timeoutDuration(timeoutDuration)
            .build();

        ExecutorService executorService = Executors.newFixedThreadPool(tasksNum);
        List<Duration> times = synchronizedList(new ArrayList<>(9));

        RateLimiter limiter = buildRateLimiter(config);
        RateLimiter.Metrics metrics = limiter.getMetrics();
        waitForRefresh(metrics, config, '$');

        LocalDateTime testStart = LocalDateTime.now();
        Runnable runnable = RateLimiter.decorateRunnable(limiter, () -> {
            times.add(Duration.between(testStart, LocalDateTime.now()));
        });
        for (int i = 0; i < tasksNum; i++) {
            executorService.submit(runnable);
        }

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(durationToWait.toMillis(), TimeUnit.MILLISECONDS);
        then(terminated).isTrue();


        ArrayList<Long> runningDeltas = new ArrayList<>();
        long previousDuration = times.get(0).toMillis();
        for (Duration time : times) {
            long current = time.toMillis();
            long delta = Math.abs(previousDuration - current);
            runningDeltas.add(delta);
            previousDuration = current;
        }

        then(runningDeltas.get(0)).isZero();
        then(runningDeltas.get(1)).isLessThan(20);
        then(runningDeltas.get(2)).isLessThan(20);
        then(runningDeltas.get(3)).isBetween(200L, 1050L);
        then(runningDeltas.get(4)).isLessThan(20);
        then(runningDeltas.get(5)).isLessThan(20);
        then(times).hasSize(6);
    }

    protected void waitForRefresh(RateLimiter.Metrics metrics, RateLimiterConfig config,
                                  char printedWhileWaiting) {
        Instant start = Instant.now();
        while (Instant.now().isBefore(start.plus(config.getLimitRefreshPeriod()))) {
            try {
                if (metrics.getAvailablePermissions() == config.getLimitForPeriod()) {
                    break;
                }
                System.out.print(printedWhileWaiting);
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        System.out.println();
    }
}
