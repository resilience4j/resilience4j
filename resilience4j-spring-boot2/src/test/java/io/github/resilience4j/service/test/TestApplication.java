package io.github.resilience4j.service.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author bstorozhuk
 */
@SpringBootApplication(scanBasePackages = {
        "io.github.resilience4j.circuitbreaker.autoconfigure",
        "io.github.resilience4j.ratelimiter.autoconfigure",
        "io.github.resilience4j.bulkhead.autoconfigure",
        "io.github.resilience4j.retry.configure",
        "io.github.resilience4j.service.test"
})
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
