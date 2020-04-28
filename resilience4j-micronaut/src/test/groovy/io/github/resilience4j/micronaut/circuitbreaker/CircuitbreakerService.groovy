package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.micronaut.TestDummyService
import io.micronaut.http.annotation.Controller

@Controller("/circuitbreaker")
class CircuitbreakerService extends TestDummyService {
}
