package io.github.resilience4j;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

import static io.github.resilience4j.TestDummyService.BACKEND;

@SpringBootApplication
@Configuration
public class TestCustomRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestCustomRegistryApplication.class, args);
    }

    @Bean
    @Primary
    public RateLimiterRegistry rateLimiterRegistry() {

        RateLimiterConfig backendRateLimiterConfig = RateLimiterConfig.custom()
                .limitForPeriod(1)
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .timeoutDuration(Duration.ofMillis(1))
                .build();

        return RateLimiterRegistry.custom()
                .withRateLimiterConfig(RateLimiterConfig.ofDefaults())
                .addRateLimiterConfig(BACKEND, backendRateLimiterConfig)
                .build();
    }

}
