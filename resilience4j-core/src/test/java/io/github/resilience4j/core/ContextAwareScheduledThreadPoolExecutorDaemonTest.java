package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextAwareScheduledThreadPoolExecutorDaemonTest {

    @Test
    public void shouldCreateDaemonThreadsWhenDaemonTrue() throws Exception {
        ContextAwareScheduledThreadPoolExecutor executor =
            ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
                .corePoolSize(1)
                .daemon(true)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        executor.schedule(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);

        latch.await(5, TimeUnit.SECONDS);
        assertThat(wasDaemon.get()).isTrue();
        executor.shutdown();
    }

    @Test
    public void shouldCreateNonDaemonThreadsByDefault() throws Exception {
        ContextAwareScheduledThreadPoolExecutor executor =
            ContextAwareScheduledThreadPoolExecutor.newScheduledThreadPool()
                .corePoolSize(1)
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        executor.schedule(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
            latch.countDown();
        }, 0, TimeUnit.MILLISECONDS);

        latch.await(5, TimeUnit.SECONDS);
        assertThat(wasDaemon.get()).isFalse();
        executor.shutdown();
    }
}
