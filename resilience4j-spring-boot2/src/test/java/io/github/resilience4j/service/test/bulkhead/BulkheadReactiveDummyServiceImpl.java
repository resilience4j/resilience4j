package io.github.resilience4j.service.test.bulkhead;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Bulkhead(name = BulkheadReactiveDummyService.BACKEND)
@Component
public class BulkheadReactiveDummyServiceImpl implements BulkheadReactiveDummyService {

    private static final Logger logger = LoggerFactory
        .getLogger(BulkheadReactiveDummyServiceImpl.class);

    @Override
    public Flux<String> doSomethingFlux() {
        return Flux.just("test").delayElements(Duration.ofMillis(500));
    }

    @Override
    public Flowable<String> doSomethingFlowable() {
        return Flowable.just("testMaybe").delay(500, TimeUnit.MILLISECONDS);
    }
}
