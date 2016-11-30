package javaslang.ratelimiter.internal;

import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author bstorozhuk
 */
public class TimeBasedRateLimiterTest {

    public static final int N_THREADS = 4;
    public static final AtomicLong counter = new AtomicLong(0);
    public static final AtomicBoolean required = new AtomicBoolean(false);

    @Test
    public void test() throws Exception {
        RateLimiterConfig config = RateLimiterConfig.builder()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMillis(500))
            .timeoutDuration(Duration.ZERO)
            .build();
        RateLimiter limiter = new AtomicRateLimiter("test", config);

        Runnable guarded = () -> {
            if (limiter.getPermission(Duration.ofSeconds(10))) {
                counter.incrementAndGet();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        for (int i = 0; i < N_THREADS; i++) {
            pool.execute(() -> {
                while (true) {
                    if (required.get()) {
                        guarded.run();
                    }
                }
            });
        }
        required.set(true);
        Thread.sleep(2200);
        required.set(false);
        System.out.println("COUNTER: " + counter);
        pool.shutdownNow();
    }
}