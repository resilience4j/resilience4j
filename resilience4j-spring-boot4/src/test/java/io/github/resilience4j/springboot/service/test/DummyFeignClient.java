package io.github.resilience4j.springboot.service.test;


import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(url = "localhost:8090", name = "dummyFeignClient")
@CircuitBreaker(name = "dummyFeignClient")
@Bulkhead(name = "dummyFeignClient")
public interface DummyFeignClient {

    @GetMapping(path = "/sample/{error}")
    void doSomething(@PathVariable(name = "error") String error);
}
