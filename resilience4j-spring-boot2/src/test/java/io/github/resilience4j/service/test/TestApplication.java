package io.github.resilience4j.service.test;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

/**
 * @author bstorozhuk
 */
@SpringBootApplication
@EnableFeignClients
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public CircuitBreakerConfigCustomizer testCustomizer() {
        return new CircuitBreakerConfigCustomizer() {
            @Override
            public void customize(CircuitBreakerConfig.Builder builder) {
                builder.slidingWindowSize(100);
            }

            @Override
            public String name() {
                return "backendC";
            }
        };

    }
}
