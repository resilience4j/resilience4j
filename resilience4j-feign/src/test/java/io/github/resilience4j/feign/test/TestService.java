package io.github.resilience4j.feign.test;

import feign.RequestLine;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.ratelimiter.RateLimiter;


public interface TestService {

    @RequestLine("GET /greeting")
    String greeting();

    static TestService create(String url, RateLimiter rateLimiter) {
        FeignDecorators decorators = FeignDecorators.builder()
            .withRateLimiter(rateLimiter)
            .build();
        return Resilience4jFeign.builder(decorators)
            .target(TestService.class, url);
    }
}
