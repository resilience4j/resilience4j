package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class NamingThreadFactoryTest {

    @Test
    public void shouldCreateDaemonThreadWhenDaemonTrue() {
        NamingThreadFactory factory = new NamingThreadFactory("test", true);
        AtomicBoolean wasDaemon = new AtomicBoolean(false);

        Thread thread = factory.newThread(() -> {
            wasDaemon.set(Thread.currentThread().isDaemon());
        });

        assertThat(thread.isDaemon()).isTrue();
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
