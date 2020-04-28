package io.github.resilience4j.micronaut.retry

import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.http.annotation.Controller

@Controller("/retry")
class RetryService extends TestDummyService{


}
