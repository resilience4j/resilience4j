package io.github.resilience4j.springboot.service.test.retry;


import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import static io.github.resilience4j.springboot.service.test.retry.RetryDummyFeignClient.RETRY_DUMMY_FEIGN_CLIENT_NAME;

@FeignClient(url = "localhost:8090", name = RETRY_DUMMY_FEIGN_CLIENT_NAME)
@Retry(name = RETRY_DUMMY_FEIGN_CLIENT_NAME)
public interface RetryDummyFeignClient {

    String RETRY_DUMMY_FEIGN_CLIENT_NAME = "retryDummyFeignClient";

    @GetMapping(path = "/retry/{error}")
    void doSomething(@PathVariable(name = "error") String error);
}
