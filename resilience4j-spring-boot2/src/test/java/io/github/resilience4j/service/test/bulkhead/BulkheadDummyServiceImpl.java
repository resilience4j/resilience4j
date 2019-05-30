package io.github.resilience4j.service.test.bulkhead;

import org.springframework.stereotype.Component;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;

@Bulkhead(name = BulkheadDummyService.BACKEND)
@Component
public class BulkheadDummyServiceImpl implements BulkheadDummyService {
    @Override
    public void doSomething() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            //do nothing
        }
    }
}
