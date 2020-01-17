package io.github.resilience4j.timelimiter.autoconfigure;

import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.fallback.FallbackDecorators;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.github.resilience4j.timelimiter.configure.TimeLimiterAspect;
import io.github.resilience4j.timelimiter.configure.TimeLimiterAspectExt;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfiguration;
import io.github.resilience4j.timelimiter.configure.TimeLimiterConfigurationProperties;
import io.github.resilience4j.timelimiter.event.TimeLimiterEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        TimeLimiterConfigurationOnMissingBeanTest.ConfigWithOverrides.class,
        TimeLimiterAutoConfiguration.class,
        TimeLimiterConfigurationOnMissingBean.class
})
@EnableConfigurationProperties(TimeLimiterProperties.class)
public class TimeLimiterConfigurationOnMissingBeanTest {

    @Autowired
    public ConfigWithOverrides configWithOverrides;

    @Autowired
    private TimeLimiterRegistry timeLimiterRegistry;

    @Autowired
    private TimeLimiterAspect timeLimiterAspect;

    @Autowired
    private EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry;

    @Test
    public void testAllBeansFromTimeLimiterConfigurationHasOnMissingBean() throws NoSuchMethodException {
        final Class<TimeLimiterConfiguration> originalClass = TimeLimiterConfiguration.class;
        final Class<TimeLimiterConfigurationOnMissingBean> onMissingBeanClass = TimeLimiterConfigurationOnMissingBean.class;

        for (Method methodTimeLimiterConfiguration : originalClass.getMethods()) {
            if (methodTimeLimiterConfiguration.isAnnotationPresent(Bean.class)) {
                final Method methodOnMissing = onMissingBeanClass
                        .getMethod(methodTimeLimiterConfiguration.getName(), methodTimeLimiterConfiguration.getParameterTypes());

                assertThat(methodOnMissing.isAnnotationPresent(Bean.class)).isTrue();

                if (!"timeLimiterEventsConsumerRegistry".equals(methodOnMissing.getName()) &&
                        !"timeLimiterRegistryEventConsumer".equals(methodOnMissing.getName())) {
                    assertThat(methodOnMissing.isAnnotationPresent(ConditionalOnMissingBean.class)).isTrue();
                }
            }
        }
    }

    @Test
    public void testAllCircuitBreakerConfigurationBeansOverridden() {
        assertEquals(timeLimiterRegistry, configWithOverrides.timeLimiterRegistry);
        assertEquals(timeLimiterAspect, configWithOverrides.timeLimiterAspect);
        assertNotEquals(timeLimiterEventsConsumerRegistry, configWithOverrides.timeLimiterEventsConsumerRegistry);
    }

    @Configuration
    public static class ConfigWithOverrides {

        private TimeLimiterRegistry timeLimiterRegistry;

        private TimeLimiterAspect timeLimiterAspect;

        private EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry;

        @Bean
        public TimeLimiterRegistry timeLimiterRegistry() {
            timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
            return timeLimiterRegistry;
        }

        @Bean
        public TimeLimiterAspect timeLimiterAspect(TimeLimiterRegistry timeLimiterRegistry, @Autowired(required = false) List<TimeLimiterAspectExt> timeLimiterAspectExtList, FallbackDecorators fallbackDecorators) {
            timeLimiterAspect = new TimeLimiterAspect(timeLimiterRegistry, new TimeLimiterConfigurationProperties(), timeLimiterAspectExtList, fallbackDecorators);
            return timeLimiterAspect;
        }

        @Bean
        public EventConsumerRegistry<TimeLimiterEvent> timeLimiterEventsConsumerRegistry() {
            timeLimiterEventsConsumerRegistry = new DefaultEventConsumerRegistry<>();
            return timeLimiterEventsConsumerRegistry;
        }
    }

}
