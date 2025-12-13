package io.github.resilience4j.springboot3.verifier.autoconfigure;

import io.github.resilience4j.springboot3.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import io.github.resilience4j.springboot3.timelimiter.autoconfigure.TimeLimiterAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(before = {
        BulkheadAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class,
        RateLimiterAutoConfiguration.class,
        RetryAutoConfiguration.class,
        TimeLimiterAutoConfiguration.class,
        BulkheadAutoConfiguration.class,
})
public class SpringBoot3VerifierAutoConfiguration {

    @Bean
    public SpringBoot3Verifier springBoot3Verifier() {
        var verifier = new SpringBoot3Verifier();
        verifier.verifyCompatibility();
        return verifier;
    }
}
