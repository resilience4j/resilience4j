package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SchedulerFactoryTest extends ThreadModeTestBase {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return threadModes();
    }

    /**
     * Constructor for parameterized tests.
     *
     * @param threadType the thread mode to test with ("platform" or "virtual")
     */
    public SchedulerFactoryTest(ThreadType threadType) {
        super(threadType);
    }

    @Before
    public void setUp() {
        setUpThreadMode(); // Set up thread mode from ThreadModeTestBase
        SchedulerFactory.getInstance().reset(); // Reset the factory before each test
    }

    @After
    public void tearDown() {
        cleanUpThreadMode(); // Clean up thread mode settings
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
        System.out.println("Running schedulerShouldRunTasksOnCorrectThreadTypeInBothModes in " + getThreadModeDescription());
        
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
            
            System.out.println("✅ Scheduler thread type test passed in " + getThreadModeDescription() + 
                             " - Virtual thread: " + taskRanOnVirtualThread);
        } finally {
            scheduler.shutdownNow();
        }
    }
}
