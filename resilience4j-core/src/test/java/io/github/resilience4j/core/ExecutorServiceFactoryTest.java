package io.github.resilience4j.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
class ExecutorServiceFactoryTest extends ThreadModeTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceFactoryTest.class);

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void shouldDetectCorrectThreadTypeBasedOnConfiguration(ThreadType threadType) {
        setUpThreadMode(threadType);
        LOG.info("Running shouldDetectCorrectThreadTypeBasedOnConfiguration in {}", getThreadModeDescription(threadType));

        ThreadType detected = ExecutorServiceFactory.getThreadType();

        if (isVirtualThreadMode(threadType)) {
            assertThat(detected)
                .as("ExecutorServiceFactory should detect virtual thread mode when configured in " + getThreadModeDescription(threadType))
                .isEqualTo(ThreadType.VIRTUAL);
        } else {
            assertThat(detected)
                .as("ExecutorServiceFactory should detect platform thread mode when not configured in " + getThreadModeDescription(threadType))
                .isEqualTo(ThreadType.PLATFORM);
        }

        LOG.info("Thread type detection test passed in {} - Thread type: {}", getThreadModeDescription(threadType), detected);
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void scheduledExecutorShouldProduceCorrectThreadType(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running scheduledExecutorShouldProduceCorrectThreadType in {}", getThreadModeDescription(threadType));

        ScheduledExecutorService executor =
            ExecutorServiceFactory.newSingleThreadScheduledExecutor("executor-test-" + threadType);

        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());

        try {
            boolean taskRanOnVirtualThread = isVirtual.get(1, TimeUnit.SECONDS);

            if (isVirtualThreadMode(threadType)) {
                assertThat(taskRanOnVirtualThread)
                    .as("Task should run on a virtual thread when configured in " + getThreadModeDescription(threadType))
                    .isTrue();
            } else {
                assertThat(taskRanOnVirtualThread)
                    .as("Task should run on a platform thread by default in " + getThreadModeDescription(threadType))
                    .isFalse();
            }

            LOG.info("Scheduled executor thread type test passed in {} - Virtual thread: {}",
                getThreadModeDescription(threadType), taskRanOnVirtualThread);
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void shouldHandleExecutorNamingConsistently(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running shouldHandleExecutorNamingConsistently in {}", getThreadModeDescription(threadType));

        ScheduledExecutorService executor =
            ExecutorServiceFactory.newSingleThreadScheduledExecutor("executor-naming-test-" + threadType);

        Future<String> threadName = executor.submit(() -> Thread.currentThread().getName());

        try {
            String actualThreadName = threadName.get(1, TimeUnit.SECONDS);

            assertThat(actualThreadName)
                .as("Thread name should include the provided prefix in " + getThreadModeDescription(threadType))
                .containsIgnoringCase("executor-naming-test-" + threadType);

            LOG.info("Executor naming test passed in {} - Thread name: {}", getThreadModeDescription(threadType), actualThreadName);
        } finally {
            executor.shutdownNow();
        }
    }
}
