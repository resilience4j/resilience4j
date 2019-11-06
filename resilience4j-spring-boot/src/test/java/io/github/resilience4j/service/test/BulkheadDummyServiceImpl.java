package io.github.resilience4j.service.test;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import org.springframework.stereotype.Component;

@Bulkhead(name = BulkheadDummyService.BACKEND)
@Component
public class BulkheadDummyServiceImpl implements BulkheadDummyService {

    @Override
    public void doSomething() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            //do nothing
        }
    }
}
