package io.github.resilience4j.core;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
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
@ExtendWith(ThreadModeExtension.class)
class ExecutorServiceFactoryTest {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorServiceFactoryTest.class);

    @TestTemplate
    void shouldDetectCorrectThreadTypeBasedOnConfiguration(ThreadType threadType) {
        LOG.info("Running shouldDetectCorrectThreadTypeBasedOnConfiguration in {}", threadType);

        ThreadType detected = ExecutorServiceFactory.getThreadType();

        if (threadType == ThreadType.VIRTUAL) {
            assertThat(detected)
                .as("ExecutorServiceFactory should detect virtual thread mode when configured in %s", threadType)
                .isEqualTo(ThreadType.VIRTUAL);
        } else {
            assertThat(detected)
                .as("ExecutorServiceFactory should detect platform thread mode when not configured in %s", threadType)
                .isEqualTo(ThreadType.PLATFORM);
        }

        LOG.info("Thread type detection test passed in {} - Thread type: {}", threadType, detected);
    }

    @TestTemplate
    void scheduledExecutorShouldProduceCorrectThreadType(ThreadType threadType) throws Exception {
        LOG.info("Running scheduledExecutorShouldProduceCorrectThreadType in {}", threadType);

        ScheduledExecutorService executor =
            ExecutorServiceFactory.newSingleThreadScheduledExecutor("executor-test-" + threadType);

        Future<Boolean> isVirtual = executor.submit(() -> Thread.currentThread().isVirtual());

        try {
            boolean taskRanOnVirtualThread = isVirtual.get(1, TimeUnit.SECONDS);

            if (threadType == ThreadType.VIRTUAL) {
                assertThat(taskRanOnVirtualThread)
                    .as("Task should run on a virtual thread when configured in %s", threadType)
                    .isTrue();
            } else {
                assertThat(taskRanOnVirtualThread)
                    .as("Task should run on a platform thread by default in %s", threadType)
                    .isFalse();
            }

            LOG.info("Scheduled executor thread type test passed in {} - Virtual thread: {}",
                threadType, taskRanOnVirtualThread);
        } finally {
            executor.shutdownNow();
        }
    }

    @TestTemplate
    void shouldHandleExecutorNamingConsistently(ThreadType threadType) throws Exception {
        LOG.info("Running shouldHandleExecutorNamingConsistently in {}", threadType);

        ScheduledExecutorService executor =
            ExecutorServiceFactory.newSingleThreadScheduledExecutor("executor-naming-test-" + threadType);

        Future<String> threadName = executor.submit(() -> Thread.currentThread().getName());

        try {
            String actualThreadName = threadName.get(1, TimeUnit.SECONDS);

            assertThat(actualThreadName)
                .as("Thread name should include the provided prefix in %s", threadType)
                .containsIgnoringCase("executor-naming-test-" + threadType);

            LOG.info("Executor naming test passed in {} - Thread name: {}", threadType, actualThreadName);
        } finally {
            executor.shutdownNow();
        }
    }
}
