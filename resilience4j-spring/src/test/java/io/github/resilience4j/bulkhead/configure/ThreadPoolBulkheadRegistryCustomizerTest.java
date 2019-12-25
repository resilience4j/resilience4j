package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.TestThreadLocalContextPropagator;
import io.github.resilience4j.bulkhead.ContextPropagator;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import io.github.resilience4j.customizer.CompositeRegistryCustomizer;
import io.github.resilience4j.customizer.Customizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ThreadPoolBulkheadRegistryCustomizerTest.Config.class})
public class ThreadPoolBulkheadRegistryCustomizerTest {

    @Autowired
    private ThreadPoolBulkheadRegistry registry;

    @Autowired
    private Customizer<ThreadPoolBulkheadRegistry> customizer;

    @Test
    public void testThreadPoolBulkheadCustomizer() {

        assertThat(registry).isNotNull();
        assertThat(customizer).isNotNull();


        //All Customizer bean should be added to CompositeRegistryCustomizer as its primary bean.
        assertThat(customizer.getClass()).isEqualTo(CompositeRegistryCustomizer.class);
        List<Customizer> delegates = (List<Customizer>) getField(customizer, "delegates");
        assertThat(delegates).isNotNull();
        assertThat(delegates).hasSize(2);

        //This test context propagator set to config by properties. R4J will invoke default
        // constructor of ContextPropagator class using reflection.
        //downside is that no dependencies can be added to ContextPropagators class
        ThreadPoolBulkhead bulkheadA = registry.bulkhead("bulkheadA", "backendA");
        assertThat(bulkheadA).isNotNull();
        assertThat(bulkheadA.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadA.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadA.getBulkheadConfig().getContextPropagator()).hasSize(1);
        assertThat(bulkheadA.getBulkheadConfig().getContextPropagator().get(0))
            .isInstanceOf(TestThreadLocalContextPropagator.class);

        //This test context propagator bean set to config using Customizer interface via SpringContext
        ThreadPoolBulkhead bulkheadB = registry.bulkhead("bulkheadB", "backendB");
        assertThat(bulkheadB).isNotNull();
        assertThat(bulkheadB.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadB.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadB.getBulkheadConfig().getContextPropagator()).hasSize(1);
        assertThat(bulkheadB.getBulkheadConfig().getContextPropagator().get(0))
            .isInstanceOf(BeanContextPropagator.class);

        //This test has no context propagator
        ThreadPoolBulkhead bulkheadC = registry.bulkhead("bulkheadC", "backendC");
        assertThat(bulkheadC).isNotNull();
        assertThat(bulkheadC.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadC.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadC.getBulkheadConfig().getContextPropagator()).hasSize(0);

        //This test context propagator bean set to config using Customizer interface via SpringContext
        ThreadPoolBulkhead bulkheadD = registry.bulkhead("bulkheadD", "backendD");
        assertThat(bulkheadD).isNotNull();
        assertThat(bulkheadD.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadD.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator()).hasSize(1);
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator().get(0))
            .isInstanceOf(BeanContextPropagator.class);

    }

    @Configuration
    @Import(ThreadPoolBulkheadConfiguration.class)
    static class Config {


        @Bean
        public EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry() {
            return new DefaultEventConsumerRegistry<>();
        }

        @Bean
        public Customizer<ThreadPoolBulkheadRegistry> customizerB(
            List<? extends ContextPropagator> contextPropagators) {
            return (registry) -> registry.getConfiguration("backendB")
                .orElseThrow(
                    () -> new IllegalArgumentException("backendC not present in the context"))
                .setContextPropagators(contextPropagators);
        }

        @Bean
        public Customizer<ThreadPoolBulkheadRegistry> customizerD(
            List<? extends ContextPropagator> contextPropagators) {
            return (registry) -> registry.getConfiguration("backendD")
                .orElseThrow(
                    () -> new IllegalArgumentException("backendD not present in the context"))
                .setContextPropagators(contextPropagators);
        }

        @Bean
        public BeanContextPropagator beanContextPropagator() {
            return new BeanContextPropagator();
        }

        @Bean
        public ThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties() {
            return new ThreadPoolBulkheadConfigurationPropertiesTest();
        }

        private class ThreadPoolBulkheadConfigurationPropertiesTest extends
            ThreadPoolBulkheadConfigurationProperties {

            ThreadPoolBulkheadConfigurationPropertiesTest() {
                InstanceProperties properties1 = new InstanceProperties();
                properties1.setCoreThreadPoolSize(2);
                properties1.setMaxThreadPoolSize(4);
                properties1.setContextPropagator(TestThreadLocalContextPropagator.class);
                getConfigs().put("backendA", properties1);

                InstanceProperties properties2 = new InstanceProperties();
                properties2.setCoreThreadPoolSize(2);
                properties2.setMaxThreadPoolSize(4);
                getConfigs().put("backendB", properties2);

                InstanceProperties properties3 = new InstanceProperties();
                properties3.setCoreThreadPoolSize(2);
                properties3.setMaxThreadPoolSize(4);
                getConfigs().put("backendC", properties3);

                InstanceProperties properties4 = new InstanceProperties();
                properties4.setCoreThreadPoolSize(2);
                properties4.setMaxThreadPoolSize(4);
                getConfigs().put("backendD", properties3);
            }

        }
    }

    public static class BeanContextPropagator implements ContextPropagator<String> {

        @Override
        public Supplier<Optional<String>> retrieve() {
            return () -> Optional.empty();
        }

        @Override
        public Consumer<Optional<String>> copy() {
            return (t) -> {
            };
        }

        @Override
        public Consumer<Optional<String>> clear() {
            return (t) -> {
            };
        }
    }
}
