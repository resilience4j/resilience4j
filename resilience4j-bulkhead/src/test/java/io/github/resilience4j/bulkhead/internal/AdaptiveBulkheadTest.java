package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.BulkheadAdaptationConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AdaptiveBulkheadTest {
    public static final ExecutorService ex = Executors.newFixedThreadPool(20);

    public void fun() {
        Future<?> task = ex.submit(() -> {
            try {
                Thread.sleep(AdaptiveBulkhead.sleepMs.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        try {
            task.get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void integration() throws Exception {
        BulkheadAdaptationConfig config = new BulkheadAdaptationConfig(
                40, 0.5d,
                1.0d, Duration.ofSeconds(10), Duration.ofSeconds(120)
        );
        AdaptiveBulkhead b = new AdaptiveBulkhead("testB", config);
        SemaphoreBulkhead mainB = new SemaphoreBulkhead(
                "testA",
                BulkheadConfig.custom().maxWaitTime(1000000).maxConcurrentCalls(AdaptiveBulkhead.testConcurrency.get()).build()
        );

        for (int i = 0; i < 100; i++) {
            Thread thread = new Thread(() -> {
                while (true) {
                    if (mainB.isCallPermitted()) {
                        if (b.isCallPermitted()) {
                            long start = System.nanoTime();
                            fun();
                            b.onComplete(System.nanoTime() - start);
                        }
                        mainB.onComplete();
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }

        while (true) {
            for (int i = 400; i <= 1200; i += 100) {
                AdaptiveBulkhead.sleepMs.set(i);
                Thread.sleep(300000);
            }
            AdaptiveBulkhead.testConcurrency.incrementAndGet();
            mainB.changeConfig(
                    BulkheadConfig.custom().maxWaitTime(1000000).maxConcurrentCalls(AdaptiveBulkhead.testConcurrency.get()).build()
            );
        }
    }
}
