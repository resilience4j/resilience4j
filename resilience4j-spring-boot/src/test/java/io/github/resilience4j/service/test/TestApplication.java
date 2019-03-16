package io.github.resilience4j.service.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.prometheus.client.spring.boot.EnablePrometheusEndpoint;
import io.prometheus.client.spring.boot.EnableSpringBootMetricsCollector;

/**
 * @author bstorozhuk
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.resilience4j.circuitbreaker.autoconfigure",
        "io.github.resilience4j.ratelimiter.autoconfigure",
        "io.github.resilience4j.circuitbreaker.monitoring.endpoint",
        "io.github.resilience4j.ratelimiter.monitoring.endpoint",
        "io.github.resilience4j.retry.autoconfigure",
        "io.github.resilience4j.retry.configure",
        "io.github.resilience4j.retry.monitoring.endpoint",
        "io.github.resilience4j.service.test"
})
@EnableSpringBootMetricsCollector
@EnablePrometheusEndpoint
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
