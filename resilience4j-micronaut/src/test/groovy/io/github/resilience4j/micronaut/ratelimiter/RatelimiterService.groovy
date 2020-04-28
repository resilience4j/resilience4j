package io.github.resilience4j.micronaut.ratelimiter

import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.http.annotation.Controller

@Controller("/ratelimiter")
class RatelimiterService extends TestDummyService {
}
