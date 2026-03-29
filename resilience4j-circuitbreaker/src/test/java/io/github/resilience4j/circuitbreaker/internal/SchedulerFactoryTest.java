package io.github.resilience4j.circuitbreaker.internal;

import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerFactoryTest {

    @Test
    void shouldBeSameSchedulerFactoryInstance() {
        SchedulerFactory instance = SchedulerFactory.getInstance();
        SchedulerFactory instance2 = SchedulerFactory.getInstance();
        assertThat(instance).isEqualTo(instance2);
    }

    @Test
    void shouldBeSameScheduledExecutorServiceInstance() {
        ScheduledExecutorService scheduledExecutorService = SchedulerFactory.getInstance()
            .getScheduler();
        ScheduledExecutorService scheduledExecutorService2 = SchedulerFactory.getInstance()
            .getScheduler();
        assertThat(scheduledExecutorService).isEqualTo(scheduledExecutorService2);
    }
}
