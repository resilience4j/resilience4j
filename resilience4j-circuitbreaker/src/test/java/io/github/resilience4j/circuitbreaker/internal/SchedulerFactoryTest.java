package io.github.resilience4j.circuitbreaker.internal;

import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

public class SchedulerFactoryTest {

    @Test
    public void shouldBeSameSchedulerFactoryInstance() {
        SchedulerFactory instance = SchedulerFactory.getInstance();
        SchedulerFactory instance2 = SchedulerFactory.getInstance();
        assertThat(instance).isEqualTo(instance2);
    }

    @Test
    public void shouldBeSameScheduledExecutorServiceInstance() {
        ScheduledExecutorService scheduledExecutorService = SchedulerFactory.getInstance()
            .getScheduler();
        ScheduledExecutorService scheduledExecutorService2 = SchedulerFactory.getInstance()
            .getScheduler();
        assertThat(scheduledExecutorService).isEqualTo(scheduledExecutorService2);
    }
}
