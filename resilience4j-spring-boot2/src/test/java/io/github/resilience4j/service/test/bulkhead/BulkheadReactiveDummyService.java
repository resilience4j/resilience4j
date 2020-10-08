package io.github.resilience4j.service.test.bulkhead;

import io.reactivex.Flowable;
import reactor.core.publisher.Flux;

public interface BulkheadReactiveDummyService {

    String BACKEND = "backendB";

    Flux<String> doSomethingFlux();

    Flowable<String> doSomethingFlowable();
}
