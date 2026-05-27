package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class NamingThreadFactoryTest {

    @Test
    public void shouldCreateDaemonThreadWhenDaemonTrue() throws Exception {
        NamingThreadFactory factory = new NamingThreadFactory("test", true);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        Thread thread = factory.newThread(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
        });

        thread.start();
        thread.join(TimeUnit.SECONDS.toMillis(5));

        assertThat(thread.isDaemon()).isTrue();
        assertThat(thread.isAlive()).isFalse();
        assertThat(wasDaemon.get()).isTrue();
        assertThat(thread.getName()).startsWith("test-");
    }

    @Test
    public void shouldCreateNonDaemonThreadWhenDaemonFalse() {
        NamingThreadFactory factory = new NamingThreadFactory("test", false);
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.isDaemon()).isFalse();
    }

    @Test
    public void shouldDefaultToNonDaemon() {
        NamingThreadFactory factory = new NamingThreadFactory("test");
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.isDaemon()).isFalse();
    }
}
