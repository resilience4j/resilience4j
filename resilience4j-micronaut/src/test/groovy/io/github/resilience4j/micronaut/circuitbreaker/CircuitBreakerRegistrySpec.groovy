/*
 * Copyright 2020 Michael Pollind
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.micronaut.circuitbreaker

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import spock.lang.Specification

import javax.inject.Inject

@MicronautTest
@Property(name = "resilience4j.circuitbreaker.enabled", value = "true")
@Property(name = "resilience4j.circuitbreaker.configs.default.slidingWindowType", value = "COUNT_BASED")
@Property(name = "resilience4j.circuitbreaker.configs.default.slidingWindowSize", value = "100")
@Property(name = "resilience4j.circuitbreaker.configs.default.permittedNumberOfCallsInHalfOpenState", value = "10")
@Property(name = "resilience4j.circuitbreaker.configs.default.failureRateThreshold", value = "60")
@Property(name = "resilience4j.circuitbreaker.configs.default.eventConsumerBufferSize", value = "10")
@Property(name = "resilience4j.circuitbreaker.configs.default.registerHealthIndicator", value = "true")
@Property(name = "resilience4j.circuitbreaker.instances.backendA.baseConfig", value = "default")
@Property(name = "resilience4j.circuitbreaker.instances.backendB.baseConfig", value = "default")
@Property(name = "resilience4j.circuitbreaker.instances.backendB.recordFailurePredicate", value = "io.github.resilience4j.micronaut.circuitbreaker.RecordFailurePredicate")
@Property(name = "resilience4j.circuitbreaker.instances.backendB.recordResultPredicate", value = "io.github.resilience4j.micronaut.circuitbreaker.RecordResultPredicate")
@Property(name = "resilience4j.circuitbreaker.instances.backendB.recordExceptions[0]", value = "io.github.resilience4j.micronaut.circuitbreaker.RecordedException")
@Property(name = "resilience4j.circuitbreaker.instances.backendB.ignoreExceptions[0]", value = "io.github.resilience4j.micronaut.circuitbreaker.IgnoredException")
class CircuitBreakerRegistrySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(CircuitBreakerRegistry)
        def circuitBreaker = registry.circuitBreaker("default")

        expect:
        circuitBreaker != null

        circuitBreaker.circuitBreakerConfig.slidingWindowSize == 100
        circuitBreaker.circuitBreakerConfig.slidingWindowType == CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
        circuitBreaker.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 10
        circuitBreaker.circuitBreakerConfig.failureRateThreshold == 60

    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(CircuitBreakerRegistry)
        def backendA = registry.circuitBreaker("backend-a")

        expect:
        backendA != null

        backendA.circuitBreakerConfig.slidingWindowSize == 100
        backendA.circuitBreakerConfig.slidingWindowType == CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
        backendA.circuitBreakerConfig.permittedNumberOfCallsInHalfOpenState == 10
        backendA.circuitBreakerConfig.failureRateThreshold == 60

    }

    void "backend-b configuration"() {
        given:
        def registry = applicationContext.getBean(CircuitBreakerRegistry)
        def backendB = registry.circuitBreaker("backend-b")
        def properties = applicationContext.getBean(CircuitBreakerProperties)
        def instanceProperties = properties.instances.get('backend-b')

        expect:
        backendB != null

        backendB.circuitBreakerConfig.recordExceptionPredicate.test(new IgnoredException())
        backendB.circuitBreakerConfig.recordExceptionPredicate.test(new IOException())
        backendB.circuitBreakerConfig.ignoreExceptionPredicate.test(new IgnoredException())

        instanceProperties.recordExceptions.length == 1
        instanceProperties.recordExceptions.first() == RecordedException.class

        instanceProperties.ignoreExceptions.length == 1
        instanceProperties.ignoreExceptions.first() == IgnoredException.class

        instanceProperties.recordFailurePredicate == RecordFailurePredicate.class

        instanceProperties.recordResultPredicate == RecordResultPredicate.class
    }
}
