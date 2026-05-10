package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SchedulerFactoryTest extends ThreadModeTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerFactoryTest.class);

    public SchedulerFactoryTest(ThreadType threadType) {
        super(threadType);
    }

    @Before
    public void setUp() {
        SchedulerFactory.getInstance().reset(); // Reset the factory before each test
    }

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

    @Test
    public void schedulerShouldRunTasksOnCorrectThreadTypeInBothModes() throws Exception {
        LOG.info("Running schedulerShouldRunTasksOnCorrectThreadTypeInBothModes in {}", getThreadModeDescription());

        ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();

        Future<Boolean> isVirtual = scheduler.submit(() -> Thread.currentThread().isVirtual());

        try {
            boolean taskRanOnVirtualThread = isVirtual.get(1, TimeUnit.SECONDS);
            
            if (isVirtualThreadMode()) {
                assertThat(taskRanOnVirtualThread)
                    .as("Task executed by SchedulerFactory should run on a virtual thread when configured in " + getThreadModeDescription())
                    .isTrue();
            } else {
                assertThat(taskRanOnVirtualThread)
                    .as("Task executed by SchedulerFactory should run on a platform thread by default in " + getThreadModeDescription())
                    .isFalse();
            }
            
            LOG.info("Scheduler thread type test passed in {} - Virtual thread: {}",
                getThreadModeDescription(), taskRanOnVirtualThread);
        } finally {
            scheduler.shutdownNow();
        }
    }
}
