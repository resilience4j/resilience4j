package io.github.resilience4j.service.test.bulkhead;

import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;


@Component
public class BulkheadDummyServiceImpl implements BulkheadDummyService {

    @Bulkhead(name = BulkheadDummyService.BACKEND)
    @Override
    public void doSomething() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //do nothing
        }
    }

    @Override
    @Retry(name = BulkheadDummyService.BACKEND_C)
    @Bulkhead(name = BulkheadDummyService.BACKEND_C, type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> doSomethingAsync() throws InterruptedException {
        Thread.sleep(500);
        return CompletableFuture.completedFuture("Test");
    }
}
