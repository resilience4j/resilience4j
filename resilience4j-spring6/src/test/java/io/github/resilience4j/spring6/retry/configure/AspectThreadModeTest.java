package io.github.resilience4j.spring6.retry.configure;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterAspect;
import io.github.resilience4j.spring6.timelimiter.configure.TimeLimiterConfigurationProperties;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(ThreadModeExtension.class)
class AspectThreadModeTest {

    @TestTemplate
    void shouldCreateScheduledExecutorServiceWithConfiguredThreadType() throws Exception {
        ThreadType configuredThreadType = ExecutorServiceFactory.getThreadType();

        RetryAspect retryAspect = new RetryAspect(
            new RetryConfigurationProperties(),
            RetryRegistry.ofDefaults(),
            null,
            null,
            null,
            null
        );

        Field retryExecutorField = RetryAspect.class.getDeclaredField("retryExecutorService");
        retryExecutorField.setAccessible(true);
        ScheduledExecutorService retryExecutor = (ScheduledExecutorService) retryExecutorField.get(retryAspect);

        verifyExecutorThreadType(retryExecutor, configuredThreadType);

        TimeLimiterAspect timeLimiterAspect = new TimeLimiterAspect(
            TimeLimiterRegistry.ofDefaults(),
            new TimeLimiterConfigurationProperties(),
            null,
            null,
            null,
            null
        );

        Field timeLimiterExecutorField = TimeLimiterAspect.class.getDeclaredField("timeLimiterExecutorService");
        timeLimiterExecutorField.setAccessible(true);
        ScheduledExecutorService timeLimiterExecutor = (ScheduledExecutorService) timeLimiterExecutorField.get(timeLimiterAspect);

        verifyExecutorThreadType(timeLimiterExecutor, configuredThreadType);

        retryAspect.close();
        timeLimiterAspect.close();
    }

    private void verifyExecutorThreadType(ScheduledExecutorService executor, ThreadType expectedType) throws Exception {
        AtomicBoolean isVirtual = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            isVirtual.set(Thread.currentThread().isVirtual());
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(isVirtual.get()).isEqualTo(expectedType == ThreadType.VIRTUAL);
    }
}
