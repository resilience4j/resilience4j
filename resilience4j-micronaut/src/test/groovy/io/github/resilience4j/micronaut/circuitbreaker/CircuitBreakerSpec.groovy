package io.github.resilience4j.micronaut.circuitbreaker

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.annotation.Controller
import io.micronaut.test.annotation.MicronautTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject


@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
class CircuitBreakerSpec extends Specification {
    @Inject ApplicationContext applicationContext


}
