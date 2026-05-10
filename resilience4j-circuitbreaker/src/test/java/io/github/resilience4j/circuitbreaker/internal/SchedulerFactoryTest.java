package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ThreadModeExtension.class)
class SchedulerFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerFactoryTest.class);

    @BeforeEach
    void setUp() {
        SchedulerFactory.getInstance().reset();
    }

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

    @TestTemplate
    void schedulerShouldRunTasksOnCorrectThreadTypeInBothModes(ThreadType threadType) throws Exception {
        LOG.info("Running schedulerShouldRunTasksOnCorrectThreadTypeInBothModes in {}", threadType);

        ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();

        Future<Boolean> isVirtual = scheduler.submit(() -> Thread.currentThread().isVirtual());

        try {
            boolean taskRanOnVirtualThread = isVirtual.get(1, TimeUnit.SECONDS);

            if (threadType == ThreadType.VIRTUAL) {
                assertThat(taskRanOnVirtualThread)
                    .as("Task executed by SchedulerFactory should run on a virtual thread when configured in %s", threadType)
                    .isTrue();
            } else {
                assertThat(taskRanOnVirtualThread)
                    .as("Task executed by SchedulerFactory should run on a platform thread by default in %s", threadType)
                    .isFalse();
            }

            LOG.info("Scheduler thread type test passed in {} - Virtual thread: {}", threadType, taskRanOnVirtualThread);
        } finally {
            scheduler.shutdownNow();
        }
    }
}
