package io.github.resilience4j.service.test.ratelimiter;


import static io.github.resilience4j.service.test.ratelimiter.RateLimiterDummyFeignClient.RATE_LIMITER_FEIGN_CLIENT_NAME;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(url = "localhost:8090", name = RATE_LIMITER_FEIGN_CLIENT_NAME)
@RateLimiter(name = RATE_LIMITER_FEIGN_CLIENT_NAME)
public interface RateLimiterDummyFeignClient {

    String RATE_LIMITER_FEIGN_CLIENT_NAME = "rateLimiterDummyFeignClient";

    @GetMapping(path = "/limit/{error}")
    void doSomething(@PathVariable(name = "error") String error);
}