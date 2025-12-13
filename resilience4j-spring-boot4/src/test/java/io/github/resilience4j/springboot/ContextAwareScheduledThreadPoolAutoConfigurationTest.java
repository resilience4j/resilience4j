package io.github.resilience4j.springboot;

import io.github.resilience4j.springboot.scheduled.threadpool.autoconfigure.ContextAwareScheduledThreadPoolAutoConfiguration;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ContextAwareScheduledThreadPoolAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    public void registersBeanWhenConditionalPropertyIsInKebabCase() {
        assertBeanHasBeenCreated("resilience4j.scheduled.executor.core-pool-size=1");
    }

    @Test
    public void registersBeanWhenConditionalPropertyIsInCamelCase() {
        assertBeanHasBeenCreated("resilience4j.scheduled.executor.corePoolSize=1");
    }

    @Test
    public void doesNotRegisterBeanWhenConditionalPropertyNotSpecified() {
        assertBeanWasNotCreated("randomProperty");
    }

    private void assertBeanHasBeenCreated(String conditionalProperty) {
        contextRunner(conditionalProperty).run(context ->
            assertThat(context).hasSingleBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
        );
    }

    private void assertBeanWasNotCreated(String conditionalProperty) {
        contextRunner(conditionalProperty).run(context ->
            assertThat(context).doesNotHaveBean(ContextAwareScheduledThreadPoolAutoConfiguration.class)
        );
    }

    private ApplicationContextRunner contextRunner(String conditionalProperty) {
        return contextRunner
            .withPropertyValues(conditionalProperty)
            .withConfiguration(AutoConfigurations.of(
                ContextAwareScheduledThreadPoolAutoConfiguration.class)
            );
    }


}
