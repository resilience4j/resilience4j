package io.github.resilience4j.bulkhead.configure;

import io.github.resilience4j.TestThreadLocalContextPropagator;
import io.github.resilience4j.bulkhead.*;
import io.github.resilience4j.bulkhead.configure.threadpool.ThreadPoolBulkheadConfiguration;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.common.CompositeCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigurationProperties;
import io.github.resilience4j.consumer.DefaultEventConsumerRegistry;
import io.github.resilience4j.consumer.EventConsumerRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.util.ReflectionTestUtils.getField;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {BulkheadBuilderCustomizerTest.Config.class})
public class BulkheadBuilderCustomizerTest {

    @Autowired
    private ThreadPoolBulkheadRegistry threadPoolBulkheadRegistry;

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

    @Autowired
    @Qualifier("compositeBulkheadCustomizer")
    private CompositeCustomizer<BulkheadConfigCustomizer> compositeBulkheadCustomizer;

    @Autowired
    @Qualifier("compositeThreadPoolBulkheadCustomizer")
    private CompositeCustomizer<ThreadPoolBulkheadConfigCustomizer> compositeThreadPoolBulkheadCustomizer;


    @Test
    public void testThreadPoolBulkheadCustomizer() {

        assertThat(threadPoolBulkheadRegistry).isNotNull();
        assertThat(compositeThreadPoolBulkheadCustomizer).isNotNull();

        //All Customizer bean should be added to CompositeBuilderCustomizer as its primary bean.
        Map<String, ThreadPoolBulkheadConfigCustomizer> map = (Map<String, ThreadPoolBulkheadConfigCustomizer>) getField(
            compositeThreadPoolBulkheadCustomizer,
            "customizerMap");
        assertThat(map).isNotNull();
        assertThat(map).hasSize(2).containsKeys("backendB", "backendD");

        //This test context propagator set to config by properties. R4J will invoke default
        // constructor of ContextPropagator class using reflection.
        //downside is that no dependencies can be added to ContextPropagators class
        ThreadPoolBulkhead bulkheadA = threadPoolBulkheadRegistry.bulkhead("bulkheadA", "backendA");
        assertThat(bulkheadA).isNotNull();
        assertThat(bulkheadA.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadA.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadA.getBulkheadConfig().getContextPropagator()).hasSize(1);
        assertThat(bulkheadA.getBulkheadConfig().getContextPropagator().get(0))
            .isInstanceOf(TestThreadLocalContextPropagator.class);

        //This test context propagator bean set to config using Customizer interface via SpringContext
        ThreadPoolBulkhead bulkheadB = threadPoolBulkheadRegistry.bulkhead("bulkheadB", "backendB");
        assertThat(bulkheadB).isNotNull();
        assertThat(bulkheadB.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadB.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadB.getBulkheadConfig().getContextPropagator()).hasSize(1);
        assertThat(bulkheadB.getBulkheadConfig().getContextPropagator().get(0))
            .isInstanceOf(BeanContextPropagator.class);

        //This test has no context propagator
        ThreadPoolBulkhead bulkheadC = threadPoolBulkheadRegistry.bulkhead("bulkheadC", "backendC");
        assertThat(bulkheadC).isNotNull();
        assertThat(bulkheadC.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadC.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadC.getBulkheadConfig().getContextPropagator()).hasSize(0);

        //This test context propagator bean set to config using Customizer interface via SpringContext
        ThreadPoolBulkhead bulkheadD = threadPoolBulkheadRegistry.bulkhead("bulkheadD", "backendD");
        assertThat(bulkheadD).isNotNull();
        assertThat(bulkheadD.getBulkheadConfig().getCoreThreadPoolSize()).isEqualTo(2);
        assertThat(bulkheadD.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(4);
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator()).hasSize(1);
        assertThat(bulkheadD.getBulkheadConfig().getContextPropagator().get(0))
            .isInstanceOf(BeanContextPropagator.class);

    }

    @Test
    public void testBulkheadCustomizer() {

        assertThat(bulkheadRegistry).isNotNull();
        assertThat(compositeBulkheadCustomizer).isNotNull();

        //All Customizer bean should be added to CompositeBuilderCustomizer as its primary bean.
        Map<String, ThreadPoolBulkheadConfigCustomizer> map = (Map<String, ThreadPoolBulkheadConfigCustomizer>) getField(
            compositeBulkheadCustomizer,
            "customizerMap");
        assertThat(map).isNotNull();
        assertThat(map).hasSize(1).containsKeys("backendOne");

        //This config is changed programmatically
        Bulkhead bulkheadOne = bulkheadRegistry.bulkhead("bulkheadOne", "backendOne");
        assertThat(bulkheadOne).isNotNull();
        assertThat(bulkheadOne.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(20);
        assertThat(bulkheadOne.getBulkheadConfig().getMaxWaitDuration())
            .isEqualTo(Duration.ofSeconds(1));

        Bulkhead bulkheadTwo = bulkheadRegistry.bulkhead("bulkheadTwo", "backendTwo");
        assertThat(bulkheadTwo).isNotNull();
        assertThat(bulkheadTwo.getBulkheadConfig().getMaxConcurrentCalls()).isEqualTo(10);
        assertThat(bulkheadTwo.getBulkheadConfig().getMaxWaitDuration())
            .isEqualTo(Duration.ofSeconds(1));

    }


    @Configuration
    @Import({ThreadPoolBulkheadConfiguration.class, BulkheadConfiguration.class})
    static class Config {


        @Bean
        public EventConsumerRegistry<BulkheadEvent> eventConsumerRegistry() {
            return new DefaultEventConsumerRegistry<>();
        }

        @Bean
        public ThreadPoolBulkheadConfigCustomizer customizerB(
            List<? extends ContextPropagator> contextPropagators) {
            return ThreadPoolBulkheadConfigCustomizer.of("backendB", builder ->
                builder.contextPropagator(
                    contextPropagators.toArray(new ContextPropagator[contextPropagators.size()])));
        }

        @Bean
        public ThreadPoolBulkheadConfigCustomizer customizerD(
            List<? extends ContextPropagator> contextPropagators) {
            return ThreadPoolBulkheadConfigCustomizer.of("backendD", builder ->
                builder.contextPropagator(
                    contextPropagators.toArray(new ContextPropagator[contextPropagators.size()])));
        }

        @Bean
        public BulkheadConfigCustomizer customizerOne(
            List<? extends ContextPropagator> contextPropagators) {
            return BulkheadConfigCustomizer.of("backendOne", builder ->
                builder.maxConcurrentCalls(20));
        }

        @Bean
        public BeanContextPropagator beanContextPropagator() {
            return new BeanContextPropagator();
        }

        @Bean
        public ThreadPoolBulkheadConfigurationProperties threadPoolBulkheadConfigurationProperties() {
            return new ThreadPoolBulkheadConfigurationPropertiesTest();
        }

        @Bean
        public BulkheadConfigurationProperties bulkheadConfigurationProperties() {
            return new BulkheadConfigurationPropertiesTest();
        }

        private class ThreadPoolBulkheadConfigurationPropertiesTest extends
            ThreadPoolBulkheadConfigurationProperties {

            ThreadPoolBulkheadConfigurationPropertiesTest() {
                InstanceProperties properties1 = new InstanceProperties();
                properties1.setCoreThreadPoolSize(2);
                properties1.setMaxThreadPoolSize(4);
                properties1.setContextPropagators(TestThreadLocalContextPropagator.class);
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

        private class BulkheadConfigurationPropertiesTest extends
            BulkheadConfigurationProperties {

            BulkheadConfigurationPropertiesTest() {
                InstanceProperties properties1 = new InstanceProperties();
                properties1.setMaxConcurrentCalls(10);
                properties1.setMaxWaitDuration(Duration.ofSeconds(1));
                getConfigs().put("backendOne", properties1);

                InstanceProperties properties2 = new InstanceProperties();
                properties2.setMaxConcurrentCalls(10);
                properties2.setMaxWaitDuration(Duration.ofSeconds(1));
                getConfigs().put("backendTwo", properties2);
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
