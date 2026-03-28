package io.github.resilience4j.springboot3;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.springboot3.scheduled.threadpool.autoconfigure.ContextAwareScheduledThreadPoolAutoConfiguration;
import io.github.resilience4j.springboot3.scheduled.threadpool.autoconfigure.Resilience4JThreadPoolTaskSchedulerBuilderCustomizer;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ContextAwareScheduledThreadPoolAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ContextAwareScheduledThreadPoolAutoConfiguration.class));

    @Test
    public void registersBeanWhenConditionalPropertyIsInKebabCase() {
        contextRunner.withPropertyValues("resilience4j.scheduled.executor.core-pool-size=1").run(context ->
                assertThat(context)
                        .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                        .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledThreadPoolExecutor.class).isInstanceOf(ContextAwareScheduledThreadPoolExecutor.class)
                        .extracting(ScheduledThreadPoolExecutor::getCorePoolSize).isEqualTo(1)
        );
    }

    @Test
    public void registersBeanWhenConditionalPropertyIsInCamelCase() {
        contextRunner.withPropertyValues("resilience4j.scheduled.executor.corePoolSize=1").run(context ->
                assertThat(context)
                        .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                        .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledThreadPoolExecutor.class).isInstanceOf(ContextAwareScheduledThreadPoolExecutor.class)
                        .extracting(ScheduledThreadPoolExecutor::getCorePoolSize).isEqualTo(1)
        );
    }

    @Test
    public void doesNotRegisterBeanWhenBeanAlreadyPresent() {
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = mock(ScheduledThreadPoolExecutor.class);
        contextRunner.withBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledThreadPoolExecutor.class, () -> scheduledThreadPoolExecutor)
                .run(context -> {
                            assertThat(context).getBean(ScheduledThreadPoolExecutor.class)
                                    .isNotInstanceOf(ContextAwareScheduledThreadPoolExecutor.class);
                            assertThat(context).doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.class);
                        }
                );
    }

    @Test
    public void doesNotRegisterSpringManagedBeanWhenBuilderNotPresent() {
        contextRunner.withPropertyValues(
                        "resilience4j.scheduled.executor.core-pool-size=1",
                        "resilience4j.scheduled.executor.type=spring"
                )
                .run(context -> assertThat(context)
                        .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                        .doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.SpringManagedScheduledThreadPoolConfiguration.class)
                        .doesNotHaveBean(ScheduledThreadPoolExecutor.class)
                );
    }

    @Test
    public void registersSpringManagedBeanWhenBuilderPresent() {
        contextRunner.withPropertyValues(
                        "resilience4j.scheduled.executor.core-pool-size=1",
                        "resilience4j.scheduled.executor.type=spring"
                )
                .withBean(ThreadPoolTaskSchedulerBuilder.class, ThreadPoolTaskSchedulerBuilder::new)
                .withBean(Resilience4JThreadPoolTaskSchedulerBuilderCustomizer.class, () -> builder -> builder.threadNamePrefix("fooBar"))
                .run(context -> {
                            assertThat(context)
                                    .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                                    .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.SpringManagedScheduledThreadPoolConfiguration.class);
                            assertThat(context)
                                    .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledThreadPoolExecutor.class).isNotInstanceOf(ContextAwareScheduledThreadPoolExecutor.class)
                                    .satisfies(scheduler -> {
                                        assertThat(scheduler.getCorePoolSize()).isEqualTo(1);
                                    });
                            assertThat(context)
                                    .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.SCHEDULER_NAME, ThreadPoolTaskScheduler.class)
                                    .satisfies(scheduler -> {
                                        assertThat(scheduler.getThreadNamePrefix()).isEqualTo("fooBar");
                                    });
                        }
                );
    }
}
