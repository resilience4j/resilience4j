package io.github.resilience4j.service.test.bulkhead;

import io.reactivex.*;
import reactor.core.publisher.Flux;

import java.io.IOException;

public interface BulkheadReactiveDummyService {

    String BACKEND = "backendB";

    Flux<String> doSomethingFlux();

    Flowable<String> doSomethingFlowable();

    Maybe<String> doSomethingMaybe(boolean throwException) throws IOException;

    Single<String> doSomethingSingle(boolean throwException) throws IOException;

    Completable doSomethingCompletable(boolean throwException) throws IOException;

    Observable<String> doSomethingObservable(boolean throwException) throws IOException;
}
