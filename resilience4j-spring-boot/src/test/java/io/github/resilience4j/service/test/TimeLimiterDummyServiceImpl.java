package io.github.resilience4j.service.test;

import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
public class TimeLimiterDummyServiceImpl implements TimeLimiterDummyService {

    @TimeLimiter(name = TimeLimiterDummyService.BACKEND)
    @Override
    public CompletionStage<String> doSomething(boolean timeout) throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            if (timeout) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
            return "something";
        });
    }
}
