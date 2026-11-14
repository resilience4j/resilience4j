package io.github.resilience4j.springboot3;

import io.github.resilience4j.core.ContextAwareScheduledThreadPoolExecutor;
import io.github.resilience4j.springboot3.TestThreadLocalContextPropagator.TestThreadLocalContextHolder;
import io.github.resilience4j.springboot3.scheduled.threadpool.autoconfigure.ContextAwareScheduledThreadPoolAutoConfiguration;
import io.github.resilience4j.springboot3.scheduled.threadpool.autoconfigure.Resilience4JThreadPoolTaskSchedulerBuilderCustomizer;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.ThreadPoolTaskSchedulerBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContextAwareScheduledThreadPoolAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ContextAwareScheduledThreadPoolAutoConfiguration.class));

    @AfterEach
    void cleanup() {
        TestThreadLocalContextHolder.clear();
    }

    @Test
    void registersBeanWhenConditionalPropertyIsInKebabCase() {
        contextRunner.withPropertyValues("resilience4j.scheduled.executor.core-pool-size=1").run(context ->
            assertThat(context)
                .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.Resilience4JManagedScheduledThreadPoolConfiguration.class)
                .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledExecutorService.class)
                .isInstanceOf(ContextAwareScheduledThreadPoolExecutor.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ScheduledThreadPoolExecutor.class))
                .extracting(ScheduledThreadPoolExecutor::getCorePoolSize).isEqualTo(1)
        );
    }

    @Test
    void registersBeanWhenConditionalPropertyIsInCamelCase() {
        contextRunner.withPropertyValues("resilience4j.scheduled.executor.corePoolSize=1").run(context ->
            assertThat(context)
                .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.Resilience4JManagedScheduledThreadPoolConfiguration.class)
                .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledExecutorService.class)
                .isInstanceOf(ContextAwareScheduledThreadPoolExecutor.class)
                .asInstanceOf(InstanceOfAssertFactories.type(ScheduledThreadPoolExecutor.class))
                .extracting(ScheduledThreadPoolExecutor::getCorePoolSize).isEqualTo(1)
        );
    }

    @Test
    void doesNotRegisterBeanWhenConditionalPropertyNotSpecified() {
        contextRunner.withPropertyValues("randomProperty").run(context ->
            assertThat(context)
                .doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                .doesNotHaveBean(ScheduledExecutorService.class)
        );
    }

    @Test
    void doesNotRegisterBeanWhenBeanAlreadyPresent() {
        ScheduledExecutorService scheduledExecutorService = mock(ScheduledExecutorService.class);
        contextRunner.withPropertyValues("resilience4j.scheduled.executor.core-pool-size=1")
            .withBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledExecutorService.class, () -> scheduledExecutorService)
            .run(context -> {
                assertThat(context).getBean(ScheduledExecutorService.class)
                    .isSameAs(scheduledExecutorService)
                    .isNotInstanceOf(ContextAwareScheduledThreadPoolExecutor.class);
                assertThat(context).doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.class);
            });
    }

    @Test
    void doesNotRegisterSpringManagedBeanWhenBuilderNotPresent() {
        contextRunner.withPropertyValues(
                "resilience4j.scheduled.executor.core-pool-size=1",
                "resilience4j.scheduled.executor.type=spring"
            )
            .run(context -> assertThat(context)
                .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                .doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.Resilience4JManagedScheduledThreadPoolConfiguration.class)
                .doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.SpringManagedScheduledThreadPoolConfiguration.class)
                .doesNotHaveBean(ScheduledExecutorService.class)
            );
    }

    @Test
    void registersSpringManagedBeanWhenBuilderPresent() {
        contextRunner.withPropertyValues(
                "resilience4j.scheduled.executor.core-pool-size=2",
                "resilience4j.scheduled.executor.type=spring"
            )
            .withBean(ThreadPoolTaskSchedulerBuilder.class, ThreadPoolTaskSchedulerBuilder::new)
            .withBean(Resilience4JThreadPoolTaskSchedulerBuilderCustomizer.class, () -> builder -> builder.threadNamePrefix("fooBar"))
            .run(context -> {
                assertThat(context)
                    .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
                    .doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.Resilience4JManagedScheduledThreadPoolConfiguration.class)
                    .hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.SpringManagedScheduledThreadPoolConfiguration.class);
                assertThat(context)
                    .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME, ScheduledExecutorService.class)
                    .isNotInstanceOf(ContextAwareScheduledThreadPoolExecutor.class)
                    .asInstanceOf(InstanceOfAssertFactories.type(ScheduledThreadPoolExecutor.class))
                    .satisfies(scheduler -> assertThat(scheduler.getCorePoolSize()).isEqualTo(2));
                assertThat(context)
                    .getBean(ContextAwareScheduledThreadPoolAutoConfiguration.SCHEDULER_NAME, ThreadPoolTaskScheduler.class)
                    .satisfies(scheduler -> assertThat(scheduler.getThreadNamePrefix()).isEqualTo("fooBar"));
            });
    }

    @Test
    void propagatesContextWithResilience4JManagedExecutor() {
        contextRunner.withPropertyValues(
                "resilience4j.scheduled.executor.core-pool-size=1",
                "resilience4j.scheduled.executor.context-propagators[0]=" + TestThreadLocalContextPropagator.class.getName()
            )
            .run(context -> {
                ScheduledExecutorService executor = context.getBean(
                    ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME,
                    ScheduledExecutorService.class
                );
                TestThreadLocalContextHolder.put("resilience4j-context");
                String result = executor.schedule(
                    () -> TestThreadLocalContextHolder.get().map(Object::toString).orElse(null),
                    10,
                    TimeUnit.MILLISECONDS
                ).get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("resilience4j-context");
            });
    }

    @Test
    void propagatesContextWithSpringManagedExecutorAndTaskDecorator() {
        TaskDecorator threadLocalDecorator = runnable -> {
            String captured = TestThreadLocalContextHolder.get().map(Object::toString).orElse(null);
            return () -> {
                try {
                    if (captured != null) {
                        TestThreadLocalContextHolder.put(captured);
                    }
                    runnable.run();
                } finally {
                    TestThreadLocalContextHolder.clear();
                }
            };
        };
        contextRunner.withPropertyValues(
                "resilience4j.scheduled.executor.core-pool-size=1",
                "resilience4j.scheduled.executor.type=spring"
            )
            .withBean(ThreadPoolTaskSchedulerBuilder.class, ThreadPoolTaskSchedulerBuilder::new)
            .withBean(Resilience4JThreadPoolTaskSchedulerBuilderCustomizer.class,
                () -> builder -> builder.additionalCustomizers(scheduler -> scheduler.setTaskDecorator(threadLocalDecorator)))
            .run(context -> {
                ScheduledExecutorService executor = context.getBean(
                    ContextAwareScheduledThreadPoolAutoConfiguration.EXECUTOR_NAME,
                    ScheduledExecutorService.class
                );
                TestThreadLocalContextHolder.put("spring-context");
                String result = executor.schedule(
                    () -> TestThreadLocalContextHolder.get().map(Object::toString).orElse(null),
                    10,
                    TimeUnit.MILLISECONDS
                ).get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("spring-context");
            });
    }
}
