package io.github.resilience4j.springboot.verifier.autoconfigure;

import io.github.resilience4j.springboot.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot.ratelimiter.autoconfigure.RateLimiterAutoConfiguration;
import io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration;
import io.github.resilience4j.springboot.timelimiter.autoconfigure.TimeLimiterAutoConfiguration;
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
public class SpringBoot4VerifierAutoConfiguration {

    @Bean
    public SpringBoot4Verifier springBoot4Verifier() {
        var verifier = new SpringBoot4Verifier();
        verifier.verifyCompatibility();
        return verifier;
    }
}
