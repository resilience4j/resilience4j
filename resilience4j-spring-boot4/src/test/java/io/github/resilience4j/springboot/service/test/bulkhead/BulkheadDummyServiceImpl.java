package io.github.resilience4j.springboot.service.test.bulkhead;

import io.github.resilience4j.springboot.TestThreadLocalContextPropagator;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Component
public class BulkheadDummyServiceImpl implements BulkheadDummyService {

    @Bulkhead(name = BulkheadDummyService.BACKEND)
    @Override
    public void doSomething() {

    }

    @Override
    @Retry(name = BulkheadDummyService.BACKEND_C)
    @Bulkhead(name = BulkheadDummyService.BACKEND_C, type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<String> doSomethingAsync() throws InterruptedException {
        return CompletableFuture.completedFuture("test");
    }

    @Override
    @Bulkhead(name = BulkheadDummyService.BACKEND_E, type = Bulkhead.Type.THREADPOOL)
    public CompletableFuture<Object> doSomethingAsyncWithThreadLocal() throws InterruptedException {
        return CompletableFuture.completedFuture(
            TestThreadLocalContextPropagator.TestThreadLocalContextHolder.get().orElse(null));
    }
}
