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

import io.github.resilience4j.bulkhead.ThreadPoolBulkheadRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Property
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Specification


@MicronautTest
@Property(name = "resilience4j.thread-pool-bulkhead.enabled", value = "true")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.maxThreadPoolSize", value = "10")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.coreThreadPoolSize", value = "5")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.queueCapacity", value = "4")
@Property(name = "resilience4j.thread-pool-bulkhead.configs.default.keepAliveDuration", value = "PT10S")
@Property(name = "resilience4j.thread-pool-bulkhead.instances.backendA.baseConfig", value = "default")
class ThreadPoolBulkheadRegistrySpec extends Specification {
    @Inject
    ApplicationContext applicationContext;

    void "default configuration"() {
        given:
        def registry = applicationContext.getBean(ThreadPoolBulkheadRegistry)
        def defaultBulkhead = registry.bulkhead("default")

        expect:
        defaultBulkhead != null
        defaultBulkhead.bulkheadConfig.getMaxThreadPoolSize() == 10
        defaultBulkhead.bulkheadConfig.getCoreThreadPoolSize() == 5
        defaultBulkhead.bulkheadConfig.getQueueCapacity() == 4
        defaultBulkhead.bulkheadConfig.keepAliveDuration.seconds == 10

    }

    void "instance configuration"() {
        given:
        def registry = applicationContext.getBean(ThreadPoolBulkheadRegistry)
        def backendABulkhead = registry.bulkhead("backend-a")

        expect:
        backendABulkhead != null
        backendABulkhead.bulkheadConfig.getMaxThreadPoolSize() == 10
        backendABulkhead.bulkheadConfig.getCoreThreadPoolSize() == 5
        backendABulkhead.bulkheadConfig.getQueueCapacity() == 4
        backendABulkhead.bulkheadConfig.keepAliveDuration.seconds == 10
    }
}
