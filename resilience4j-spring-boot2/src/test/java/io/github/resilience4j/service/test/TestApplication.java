package io.github.resilience4j.service.test;

import io.github.resilience4j.bulkhead.ContextPropagator;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry;
import io.github.resilience4j.customizer.Customizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author bstorozhuk
 */
@SpringBootApplication
@EnableFeignClients
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Configuration
    static class Config {

        @Bean
        public Customizer<ThreadPoolBulkheadRegistry> threadpoolBulkheadCustomizer(
            List<? extends ContextPropagator> contextPropagators) {
            return (registry) -> registry.bulkhead("backendC").getBulkheadConfig()
                .setContextPropagators(contextPropagators);
        }

        @Bean
        public ContextPropagator beanContextPropagator() {
            return new BeanContextPropagator();
        }
    }
}
