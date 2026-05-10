package io.github.resilience4j.core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parameterized tests that verify {@link ExecutorServiceFactory} respects the
 * {@code resilience4j.thread.type} system property and works correctly in both
 * platform and virtual thread modes.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class ExecutorServiceFactoryTest extends ThreadModeTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceFactoryTest.class);

    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    /**
     * Constructor for parameterized tests.
     * 
     * @param threadType the thread mode to test with ("platform" or "virtual")
     */
    public ExecutorServiceFactoryTest(ThreadType threadType) {
        super(threadType);
    }

    @Test
    public void shouldDetectCorrectThreadTypeBasedOnConfiguration() {
        LOG.info("Running shouldDetectCorrectThreadTypeBasedOnConfiguration in {}", getThreadModeDescription());

        // Test that ExecutorServiceFactory correctly detects the configured thread mode
        ThreadType threadType = ExecutorServiceFactory.getThreadType();
        boolean useVirtualThreads = threadType == ThreadType.VIRTUAL;
        
        if (isVirtualThreadMode()) {
            assertThat(threadType)
                .as("ExecutorServiceFactory should detect virtual thread mode when configured in " + getThreadModeDescription())
                .isEqualTo(ThreadType.VIRTUAL);
        } else {
            assertThat(threadType)
                .as("ExecutorServiceFactory should detect platform thread mode when not configured in " + getThreadModeDescription())
                .isEqualTo(ThreadType.PLATFORM);
        }
        
        LOG.info("Thread type detection test passed in {} - Thread type: {}", getThreadModeDescription(), threadType);
    }

    @Test
    public void scheduledExecutorShouldProduceCorrectThreadTypeInBothModes() throws Exception {
        LOG.info("Running scheduledExecutorShouldProduceCorrectThreadTypeInBothModes in {}", getThreadModeDescription());

        ScheduledExecutorService executor =
                ExecutorServiceFactory.newSingleThreadScheduledExecutor("executor-test-" + threadType);

        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());

        try {
            boolean taskRanOnVirtualThread = isVirtual.get(1, TimeUnit.SECONDS);
            
            if (isVirtualThreadMode()) {
                assertThat(taskRanOnVirtualThread)
                    .as("Task should run on a virtual thread when configured in " + getThreadModeDescription())
                    .isTrue();
            } else {
                assertThat(taskRanOnVirtualThread)
                    .as("Task should run on a platform thread by default in " + getThreadModeDescription())
                    .isFalse();
            }
            
            LOG.info("Scheduled executor thread type test passed in {} - Virtual thread: {}",
                getThreadModeDescription(), taskRanOnVirtualThread);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldHandleExecutorNamingConsistentlyInBothThreadModes() throws Exception {
        LOG.info("Running shouldHandleExecutorNamingConsistentlyInBothThreadModes in {}", getThreadModeDescription());

        ScheduledExecutorService executor =
                ExecutorServiceFactory.newSingleThreadScheduledExecutor("executor-naming-test-" + threadType);

        Future<String> threadName = executor.submit(() -> Thread.currentThread().getName());

        try {
            String actualThreadName = threadName.get(1, TimeUnit.SECONDS);
            
            // Verify thread naming includes the provided prefix
            assertThat(actualThreadName)
                .as("Thread name should include the provided prefix in " + getThreadModeDescription())
                .contains("executor-naming-test-" + threadType);
            
            // In virtual thread mode, thread names may have different patterns
            if (isVirtualThreadMode()) {
                // Virtual threads may have additional naming patterns
                assertThat(actualThreadName)
                    .as("Virtual thread name should follow expected pattern in " + getThreadModeDescription())
                    .containsIgnoringCase("executor-naming-test-" + threadType);
            } else {
                // Platform threads should follow standard naming
                assertThat(actualThreadName)
                    .as("Platform thread name should follow expected pattern in " + getThreadModeDescription())
                    .startsWith("executor-naming-test-" + threadType);
            }
            
            LOG.info("Executor naming test passed in {} - Thread name: {}", getThreadModeDescription(), actualThreadName);
        } finally {
            executor.shutdownNow();
        }
    }
}
