package io.github.resilience4j.service.test;

import io.github.resilience4j.bulkhead.ContextPropagator;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
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
        public Customizer<ThreadPoolBulkheadConfig.Builder> contextPropagatorBeanCustomizer(
            List<? extends ContextPropagator> contextPropagators) {
            return (name, builder) -> {
                if(name.equals("backendC")){
                    builder.contextPropagator(contextPropagators.toArray(new ContextPropagator[contextPropagators.size()]));
                }
            };
        }

        @Bean
        public ContextPropagator beanContextPropagator() {
            return new BeanContextPropagator();
        }
    }
}
