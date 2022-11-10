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
package io.github.resilience4j.micronaut.bulkhead

import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest
@Property(name = "resilience4j.bulkhead.enabled", value = "true")
@Property(name = "resilience4j.bulkhead.configs.default.maxConcurrentCalls", value = "2")
@Property(name = "resilience4j.bulkhead.configs.default.maxWaitDuration", value = "PT10S")
@Property(name = "resilience4j.bulkhead.instances.backendA.baseConfig", value = "default")
class BulkheadRegistrySpec extends Specification {
    @Inject
    ApplicationContext applicationContext

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(BulkheadRegistry)
        def defaultBulkhead = registry.bulkhead("default")

        expect:
        defaultBulkhead  != null
        defaultBulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        defaultBulkhead.bulkheadConfig.maxConcurrentCalls == 2
    }

    void "backend-a configuration"() {
        given:
        def registry = applicationContext.getBean(BulkheadRegistry)
        def backendABulkhead = registry.bulkhead("backend-a")


        backendABulkhead != null
        backendABulkhead.bulkheadConfig.maxWaitDuration.seconds == 10
        backendABulkhead.bulkheadConfig.maxConcurrentCalls == 2
    }
}
