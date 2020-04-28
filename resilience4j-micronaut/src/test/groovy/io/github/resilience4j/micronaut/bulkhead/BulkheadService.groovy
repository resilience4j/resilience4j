package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.annotation.Bulkhead
import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

import java.util.concurrent.CompletableFuture


@Controller("/bulkhead")
class BulkheadService extends TestDummyService {

    @Bulkhead(name = "backend-a", fallbackMethod = 'completionStageRecovery')
    @Get("/recoverable")
    public CompletableFuture<String> recoverable() {
        return asyncError();
    }

}
