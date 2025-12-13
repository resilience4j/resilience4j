package io.github.resilience4j.springboot.service.test.bulkhead;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Bulkhead(name = BulkheadReactiveDummyService.BACKEND)
@Component
public class BulkheadReactiveDummyServiceImpl implements BulkheadReactiveDummyService {

    private static final Logger logger = LoggerFactory
        .getLogger(BulkheadReactiveDummyServiceImpl.class);

    @Override
    public Flux<String> doSomethingFlux() {
        return Flux.just("test");
    }

    @Override
    public Flowable<String> doSomethingFlowable() {
        return Flowable.just("test");
    }
}
