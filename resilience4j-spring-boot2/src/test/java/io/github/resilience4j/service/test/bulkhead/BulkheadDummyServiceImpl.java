package io.github.resilience4j.service.test.bulkhead;

import io.github.resilience4j.TestThreadLocalContextPropagator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


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

    @Override
    @Bulkhead(name = BulkheadDummyService.BACKEND_D, type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Object> doSomethingAsyncWithThreadLocal() throws InterruptedException {
        return CompletableFuture.completedFuture(
            TestThreadLocalContextPropagator.TestThreadLocalContextHolder.get().orElse(null));
    }
}
