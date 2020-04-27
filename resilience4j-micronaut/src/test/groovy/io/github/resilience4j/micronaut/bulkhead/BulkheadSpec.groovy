package io.github.resilience4j.micronaut.bulkhead

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.http.annotation.Controller
import io.micronaut.test.annotation.MicronautTest
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.inject.Inject


@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
class BulkheadSpec extends Specification {
    @Inject ApplicationContext applicationContext

}
