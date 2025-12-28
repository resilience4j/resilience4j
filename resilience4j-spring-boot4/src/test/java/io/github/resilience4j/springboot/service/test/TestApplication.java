package io.github.resilience4j.springboot.service.test;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigCustomizer;
import io.github.resilience4j.common.bulkhead.configuration.ThreadPoolBulkheadConfigCustomizer;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import io.github.resilience4j.common.micrometer.configuration.TimerConfigCustomizer;
import io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigCustomizer;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import io.github.resilience4j.common.timelimiter.configuration.TimeLimiterConfigCustomizer;
import io.github.resilience4j.core.ContextPropagator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;


/**
 * @author bstorozhuk
 */
@SpringBootApplication
@EnableFeignClients
@Import(FeignAutoConfiguration.class)
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Bean
    public ThreadPoolBulkheadConfigCustomizer contextPropagatorBeanCustomizer(
            List<? extends ContextPropagator> contextPropagators) {
        return ThreadPoolBulkheadConfigCustomizer.of("backendC", (builder) ->
                builder.contextPropagator(
                        contextPropagators.toArray(new ContextPropagator[contextPropagators.size()])));
    }

    @Bean
    public BulkheadConfigCustomizer testBulkheadCustomizer() {
        return BulkheadConfigCustomizer.of(
                "backendCustomizer",
                builder -> builder.maxConcurrentCalls(20));
    }

    @Bean
    public ContextPropagator beanContextPropagator() {
        return new BeanContextPropagator();
    }

    @Bean
    public CircuitBreakerConfigCustomizer testCustomizer() {
        return CircuitBreakerConfigCustomizer
                .of("backendC", builder -> builder.slidingWindowSize(100));
    }

    @Bean
    public RateLimiterConfigCustomizer testRateLimiterCustomizer() {
        return RateLimiterConfigCustomizer
                .of("backendCustomizer", builder -> builder.limitForPeriod(200));
    }

    @Bean
    public RetryConfigCustomizer testRetryCustomizer() {
        return RetryConfigCustomizer.of("retryBackendD",
                builder -> builder.maxAttempts(4));
    }


    @Bean
    public TimeLimiterConfigCustomizer testTimeLimiterCustomizer() {
        return TimeLimiterConfigCustomizer.of("timeLimiterBackendD",
                builder -> builder.timeoutDuration(Duration.ofSeconds(3)));
    }

    @Bean
    public BulkheadConfigCustomizer testBulkheadConfigCustomizer() {
        return new BulkheadConfigCustomizer() {
            @Override
            public void customize(BulkheadConfig.Builder configBuilder) {
                configBuilder.maxConcurrentCalls(3);
            }

            @Override
            public String name() {
                return "backendD";
            }
        };
    }

    @Bean
    public TimerConfigCustomizer testTimerConfigCustomizerH() {
        return TimerConfigCustomizer.of("backendH", builder -> builder.metricNames("resilience4j.timer.callsHCustomized"));
    }

    @Bean
    public TimerConfigCustomizer testTimerConfigCustomizerI() {
        return TimerConfigCustomizer.of("backendI", builder -> builder.metricNames("resilience4j.timer.callsICustomized"));
    }
}
